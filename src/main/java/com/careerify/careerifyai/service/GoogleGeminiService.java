package com.careerify.careerifyai.service;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import com.google.cloud.vertexai.generativeai.ResponseHandler;

@Service
public class GoogleGeminiService {

    @Value("${spring.ai.vertex.ai.gemini.projectId}")
    String projectID;

    @Value("${spring.ai.vertex.ai.gemini.location}")
    String location;

    @Value("${spring.ai.vertex.ai.gemini.chat.options.model}")
    String modelName;

    public String generateContent(String textPrompt) throws IOException {
        return textInput(projectID, location, modelName, textPrompt);
    }

    private String textInput(
            String projectId, String location, String modelName, String textPrompt) throws IOException {
        try (VertexAI vertexAI = new VertexAI(projectId, location)) {
            GenerativeModel model = new GenerativeModel(modelName, vertexAI);

            GenerateContentResponse response = model.generateContent(textPrompt);
            return ResponseHandler.getText(response);
        }
    }

    public String imageTextInput(String fileName, String texString)
            throws IOException {
        try (VertexAI vertexAI = new VertexAI(projectID, location)) {

            GenerativeModel model = new GenerativeModel(modelName, vertexAI);
            GenerateContentResponse response = model.generateContent(ContentMaker.fromMultiModalData(
                    PartMaker.fromMimeTypeAndData("image/png", fileName), texString));

            return Optional.ofNullable(response.getCandidates(0).getContent().getParts(0).getText()).orElse("ResponseNull");
        }
    }

}