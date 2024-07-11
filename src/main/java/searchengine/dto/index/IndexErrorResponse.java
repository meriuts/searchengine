package searchengine.dto.index;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class IndexErrorResponse extends IndexResponse {
    @JsonProperty("error")
    private String textError;

    public IndexErrorResponse(String textError) {
        super(false);
        this.textError = textError;
    }
}
