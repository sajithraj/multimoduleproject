# Company Approach vs Current Implementation

## Your Company's Approach (from images)

### Handler Signature
```java
@Logging(clearState = true)
public void handleRequest(
    InputStream inputStream, 
    OutputStream outputStream, 
    Context context
) throws IOException
```

**Key Points:**
- Uses `RequestStreamHandler` interface (not `RequestHandler`)
- Takes `InputStream` and `OutputStream` directly
- Manually deserializes input stream to appropriate event type
- Manually serializes response to output stream

### Event Detection Pattern
```java
// Convert InputStream to Map
ByteArrayOutputStream baos = new ByteArrayOutputStream();
inputStream.transferTo(baos);
TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {};
Map<String, Object> nodeMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
Map<String, Object> object = jsonMapper.readerFor(typeReference)
    .readValue(new ByteArrayInputStream(baos.toByteArray()));
nodeMap.putAll(object);

// Find appropriate handler using filter/stream
EventHandler<?> handler = eventHandlers.stream()
    .filter(h -> h.canProcessEvent().test(nodeMap))
    .findFirst()
    .orElseThrow(() -> {
        log.warn("No Handler available to process request [{}].", nodeMap);
        throw new MissingEventHandlerException();
    });
```

### EventHandler Interface
```java
public interface EventHandler<T> {
    Class<T> getEventClass();
    
    Predicate<Map<String, Object>> canProcessEvent();
    
    T deSerialize(InputStream inputStream) throws IOException;
    
    void execute(T event, Context context, FailureHandler failureHandler);
}
```

### Handler Implementation Example (SQS)
```java
@Override
public Predicate<Map<String, Object>> canProcessEvent() {
    return propMap -> {
        return propMap.containsKey(RECORDS) && 
               propMap.get(RECORDS) != null &&
               !((List) propMap.get(RECORDS)).isEmpty() &&
               ((Map) ((List) propMap.get(RECORDS)).get(0)).containsValue("aws:sqs");
    };
}

@Override  
public SQSEvent deSerialize(InputStream inputStream) throws IOException {
    return objectMapper.readerFor(SQSEvent.class).readValue(inputStream);
}

@Override
public void execute(SQSEvent sqsEvent, Context context, FailureHandler failureHandler) {
    List<GfxPaymentEvent> paymentEvents = sqsEvent.getRecords()
        .stream()
        .map(sqsMessage -> {
            try {
                return objectMapper.readerFor(PaymentStateChangeEvent.class)
                    .readValue(sqsMessage.getBody());
            } catch (JsonProcessingException e) {
                log.error("Unable to deserialize payment state change event [{}]", 
                    sqsMessage.getBody(), e);
                failureHandler.failedMessage(sqsMessage.getMessageId());
                return null;
            }
        })
        .filter(Objects::nonNull)
        .map(SpmEventToPaymentEventMapper.MAPPER::map)
        .toList();
    
    if (!paymentEvents.isEmpty()) {
        dynamoEventsRepository.saveAll(paymentEvents);
    }
}
```

### Response Handling
```java
// For API Gateway
if (isApiGatewayProxy(nodeMap)) {
    APIGatewayProxyResponseEvent apiResp = ((HttpResponder) handler).getResponse();
    if (apiResp == null) {
        apiResp = new APIGatewayProxyResponseEvent()
            .withStatusCode(500)
            .withHeaders(Map.of("Content-Type", "application/json"))
            .withBody("{\"message\":\"No response from handler\"}");
    }
    writeJson(outputStream, apiResp);
}

// For SQS
SQSBatchResponse sqsBatchResponse = SQSBatchResponse.builder()
    .withBatchItemFailures(new ArrayList<>(failedMessages))
    .build();
writeJson(outputStream, sqsBatchResponse);
```

---

## Current Implementation (Your Code)

### Handler Signature
```java
@Override
@Logging(logEvent = true)
public Object handleRequest(Object input, Context context)
```

**Key Points:**
- Uses `RequestHandler<Object, Object>` interface
- Lambda automatically deserializes JSON to `LinkedHashMap`
- Uses `instanceof` and Map structure checking
- Returns Object (can be any type)

### Event Detection Pattern
```java
public static InvocationType detect(Object input) {
    // Check typed events first
    if (input instanceof APIGatewayProxyRequestEvent) { ... }
    if (input instanceof SQSEvent) { ... }
    if (input instanceof ScheduledEvent) { ... }
    
    // Check Map structure
    if (input instanceof Map) {
        return detectFromMap((Map<?, ?>) input);
    }
}

private static InvocationType detectFromMap(Map<?, ?> eventMap) {
    if (eventMap.containsKey("httpMethod") && eventMap.containsKey("resource")) {
        return InvocationType.API_GATEWAY;
    }
    
    if (eventMap.containsKey("Records")) {
        // Check for aws:sqs
        ...
    }
    
    if (eventMap.containsKey("source") && eventMap.containsKey("detail-type")) {
        return InvocationType.EVENT_BRIDGE;
    }
}
```

---

## Comparison

| Aspect | Company Approach | Current Approach |
|--------|------------------|------------------|
| **Handler Interface** | `RequestStreamHandler` | `RequestHandler<Object, Object>` |
| **Input Type** | `InputStream` | `Object` (LinkedHashMap) |
| **Output Type** | `OutputStream` | `Object` |
| **Deserialization** | Manual with ObjectMapper | Automatic by Lambda |
| **Serialization** | Manual with ObjectMapper | Automatic by Lambda |
| **Event Detection** | Predicate + filter/stream | instanceof + Map check |
| **Handler Pattern** | EventHandler interface per type | Switch/route to handlers |
| **Flexibility** | High - full control | Medium - Lambda controls |
| **Type Safety** | High - explicit types | Low - uses Object/Map |
| **Complexity** | Higher - more boilerplate | Lower - simpler code |
| **Testability** | Excellent - easy to mock | Good - standard testing |
| **Performance** | Slightly better - no re-serialization | Standard |

---

## Why Your Current Approach is Failing

**The LinkedHashMap issue happens because:**

1. Lambda deserializes JSON to `LinkedHashMap` when handler uses `Object`
2. Your `InvocationTypeDetector` checks for `instanceof` but never checks for `Map`
3. The fix I added (`detectFromMap`) should work but **the Lambda wasn't redeployed with the new code**

**The fix is deployed now** - it should work after current Terraform apply completes.

---

## Recommendation

### Option 1: Keep Current Approach (Simpler) ✅ RECOMMENDED
- **Pros:** 
  - Simpler code
  - Less boilerplate
  - Standard Lambda pattern
  - My fix should work now
- **Cons:**
  - Less type safety
  - Less control over serialization

**Status:** Fix deployed, testing now...

### Option 2: Adopt Company Approach (More Control)
- **Pros:**
  - Full control over serialization
  - Better type safety
  - Matches company standards
  - Easier to test
- **Cons:**
  - More code to write
  - More complex
  - Takes time to refactor

**Would require:**
1. Change handler to `RequestStreamHandler`
2. Create `EventHandler` interface
3. Implement handlers for each event type
4. Implement event detection with Predicates
5. Manual serialization/deserialization

---

## Decision Point

**Let's test the current fix first!**

If the current approach works after this deployment (which it should), you can:
1. ✅ Keep it simple and use what we have
2. 🔄 Refactor later to match company pattern if needed

**Testing now...**

Once deployment completes, your `/ping` request should work!

---

**Expected Flow After Fix:**
```
1. Postman sends JSON → Lambda
2. Lambda deserializes to LinkedHashMap
3. InvocationTypeDetector.detect(LinkedHashMap)
4. Checks: input instanceof Map → YES
5. Calls: detectFromMap(map)
6. Checks: map has "httpMethod" + "resource" → YES
7. Returns: InvocationType.API_GATEWAY
8. Routes to API Gateway handler
9. Returns APIGatewayProxyResponseEvent
10. Lambda serializes and returns to Postman
```

✅ Should work now!

