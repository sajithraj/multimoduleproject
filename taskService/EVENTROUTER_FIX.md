# EventRouter LinkedHashMap Fix

## Issue
The Lambda was throwing a `ClassCastException` when trying to cast `LinkedHashMap` to `APIGatewayProxyRequestEvent`:

```
java.lang.ClassCastException: class java.util.LinkedHashMap cannot be cast to class 
com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
```

## Root Cause
The `handleEventBridge()` method in `EventRouter.java` had an inconsistent signature compared to the other handler methods. It was accepting a `ScheduledEvent` parameter directly, but the router was passing the raw `Object` input (which is a `LinkedHashMap` when Lambda deserializes JSON).

## Solution
Updated the `handleEventBridge()` method to match the pattern used by `handleApiGateway()` and `handleSqs()`:

1. Accept `Object input` parameter
2. Check if input is already the typed event using `instanceof`
3. If already typed, use it directly; otherwise, use `EventDeserializer` to convert from LinkedHashMap

### Changes Made

**File: `taskService/src/main/java/com/project/task/router/EventRouter.java`**

**Before:**
```java
private String handleEventBridge(ScheduledEvent event, Context context) {
    log.info("Handling EventBridge event: source={}, detailType={}",
            event.getSource(), event.getDetailType());
    
    return EB_HANDLER.handle(event, context);
}
```

**After:**
```java
private String handleEventBridge(Object input, Context context) {
    // Convert LinkedHashMap to typed AWS event
    ScheduledEvent event = (input instanceof ScheduledEvent)
            ? (ScheduledEvent) input
            : EventDeserializer.toScheduledEvent(input);
    
    log.info("Handling EventBridge event: source={}, detailType={}",
            event.getSource(), event.getDetailType());
    
    return EB_HANDLER.handle(event, context);
}
```

## Pattern Consistency
Now all three event handler methods follow the same consistent pattern:

1. **handleApiGateway(Object input, Context context)**
   - Converts LinkedHashMap → APIGatewayProxyRequestEvent
   
2. **handleSqs(Object input, Context context)**
   - Converts LinkedHashMap → SQSEvent
   
3. **handleEventBridge(Object input, Context context)**
   - Converts LinkedHashMap → ScheduledEvent

## Testing
After deploying the updated Lambda:
- API Gateway invocations should work without ClassCastException
- SQS events should continue to work
- EventBridge scheduled events should continue to work

## Next Steps
1. Rebuild the Lambda JAR: `mvn clean package`
2. Deploy to AWS Lambda
3. Test with API Gateway invocation
4. Verify logs show successful processing

