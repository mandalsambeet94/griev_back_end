package com.grievance.handler;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.grievance.GrievanceApplication;

public class LambdaHandler implements RequestHandler<AwsProxyRequest, AwsProxyResponse> {

    private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    static {
        try {
            handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(
                    GrievanceApplication.class);

            // We don't need to load the application context for every Lambda invocation
            //handler.setInitializationTimeout(10_000);

        } catch (ContainerInitializationException e) {
            // If we fail here, we re-throw the exception to force another cold start
            throw new RuntimeException("Could not initialize Spring Boot application", e);
        }
    }

    @Override
    public AwsProxyResponse handleRequest(AwsProxyRequest input, Context context) {
        // Check if RDS is available, if not, return 503
        if (!isRdsAvailable()) {
            return new AwsProxyResponse(503, null,
                    "{\"message\": \"Database is starting. Please try again in 2 minutes.\"}");
        }

        return handler.proxy(input, context);
    }

    private boolean isRdsAvailable() {
        // Implement RDS availability check
        // You could check CloudWatch metrics or use a simple cache
        return true;
    }
}