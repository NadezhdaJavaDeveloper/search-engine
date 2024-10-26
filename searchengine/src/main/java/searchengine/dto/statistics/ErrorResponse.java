package searchengine.dto.statistics;

public class ErrorResponse {
    private final boolean result;
    private final String error;





    public ErrorResponse(String error) {
        this.result = false;
        this.error = error;

    }

    public String getError() {
        return error;
    }
    public boolean isResult() {
        return result;
    }
}
