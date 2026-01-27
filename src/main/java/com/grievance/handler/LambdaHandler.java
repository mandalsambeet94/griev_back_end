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
            // Initialize with default configuration
            handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(
                    GrievanceApplication.class);

            // Version 2.0.0 uses different configuration approach
            // Remove the problematic method calls
            System.out.println("Spring Boot Lambda Handler initialized successfully");

        } catch (ContainerInitializationException e) {
            System.err.println("Failed to initialize Spring Boot: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Could not initialize Spring Boot application", e);
        }
    }

    @Override
    public AwsProxyResponse handleRequest(AwsProxyRequest input, Context context) {
        try {
            // Log the request for debugging
            if (context != null) {
                context.getLogger().log("Received request: " + input.getHttpMethod() + " " + input.getPath());
            }

            return handler.proxy(input, context);
        } catch (Exception e) {
            String errorMessage = "Error in handleRequest: " + e.getMessage();
            if (context != null) {
                context.getLogger().log(errorMessage);
            } else {
                System.err.println(errorMessage);
            }
            e.printStackTrace();

            return new AwsProxyResponse(500, null,
                    "{\"message\": \"Internal server error\", \"error\": \"" +
                            e.getMessage().replace("\"", "'") + "\"}");
        }
    }
}