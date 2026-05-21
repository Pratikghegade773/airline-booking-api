package com.airlines.go7api.error;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
public class ErrorRsp {

    @Data
    @AllArgsConstructor
    public static class Error {
        private String error;
        private String code;
        private String type;
        private String cause;

        public Error() {
        }

        public Error(String message, String number, String Forbidden) {
            this.error = message;
            this.code = number;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public void setCause(String cause) {
            this.cause = cause;
        }

        public String getCause() {
            return cause;
        }
    }

    @Getter
    @Setter
    private String airlineCode;
    @Getter
    @Setter
    private String path;
    @Getter
    @Setter
    private String source;

    private List<Error> errorList;

    public ErrorRsp() {
        this.errorList = new ArrayList<>();
    }

    public List<Error> getErrorList() {
        return this.errorList;
    }
}
