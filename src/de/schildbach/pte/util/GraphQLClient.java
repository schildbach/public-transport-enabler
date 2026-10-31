/*
 * Copyright the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.util;

import de.schildbach.pte.exception.ParserException;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper class to run POST request for GraphQL endpoints
 */
public class GraphQLClient {

    private final HttpClient httpClient;
    public GraphQLClient(final HttpClient httpClient) {
        this.httpClient = httpClient;
        //fix the default headers for HttpClient
        httpClient.setHeader("Accept","application/json");
    }

    @NotNull
    public JSONObject makeRequest(@NotNull String urlString,@NotNull String operationName,@NotNull Map<String, Object> variables,@NotNull String query) throws IOException {
        JSONObject qar;
        try {
            qar = new JSONObject();
            qar.put("operationName", operationName);
            qar.put("variables", new JSONObject(variables));
            qar.put("query", query);
        } catch (JSONException e) {
            throw new ParserException("cannot parse add objects to GraphQL request, operationName: "+operationName +", variables: "+variables, e);
        }

        String jsonPayload = qar.toString();
        HttpUrl url = HttpUrl.parse(urlString);

        JSONObject res;
        String result = httpClient.get(url, jsonPayload, "application/json").toString();
        try {
            res = new JSONObject(result);
        } catch (JSONException exc){
            throw new ParserException("cannot parse json: '" + result + "' on " + url, exc);
        }

        return res;
    }

    public static int countErrors(JSONObject result) throws JSONException {
        if (result.has("errors"))
            return result.getJSONArray("errors").length();
        else return  0;
    }

    @Nullable
    public static GraphQLError getError(JSONObject result, int errorIdx) throws JSONException {
        if(result.has("errors")){
            return GraphQLError.fromJson(result.getJSONArray("errors").getJSONObject(errorIdx));
        } else{
            return null;
        }
    }

    public static class GraphQLError {
        private final String message;
        private final String errorType;
        private final List<String> errorStack;


        public GraphQLError(String message, String errorType, List<String> stack) {
            this.message = message;
            this.errorType = errorType;
            this.errorStack = stack;
        }

        public String getMessage() {
            return message;
        }

        public String getErrorType() {
            return errorType;
        }

        public String getErrorStack(){
            if (errorStack.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            sb.append(errorStack.get(0));
            for (int i=1; i<errorStack.size(); ++i) {
                sb.append("\n");
                sb.append(errorStack.get(i));
            }
            return sb.toString();
        }

        public static GraphQLError fromJson(JSONObject error) throws JSONException {
            ArrayList<String> stack = new ArrayList<>();
            JSONArray arr = error.getJSONArray("stack");
            for (int i=0; i<arr.length(); ++i){
                stack.add(arr.getString(i));
            }
            return  new GraphQLError(error.getString("message"), error.getString("errorType"), stack);
        }

    }
}
