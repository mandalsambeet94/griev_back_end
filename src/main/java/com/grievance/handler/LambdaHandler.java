package com.grievance.handler;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.grievance.GrievanceApplication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LambdaHandler implements RequestStreamHandler {

    private static final SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    static {
        try {
            handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(GrievanceApplication.class);

            // Correct way to register binary content types
            handler.getContainerConfig().addBinaryContentTypes(
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/octet-stream"
            );

        } catch (ContainerInitializationException e) {
            throw new RuntimeException("Could not initialize Spring Boot application", e);
        }
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        handler.proxyStream(inputStream, outputStream, context);
    }
}

/*
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
            handler.onStartup(servletContext -> {
                servletContext.setInitParameter(
                        "binaryContentTypes",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/octet-stream"
                );
            });

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
}*/
