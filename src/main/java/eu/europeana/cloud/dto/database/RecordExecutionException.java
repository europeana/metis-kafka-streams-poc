package eu.europeana.cloud.dto.database;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@AllArgsConstructor
public class RecordExecutionException {
    @SerializedName("execution_name")
    private String executionName;
    @SerializedName("exception_name")
    private String exceptionName;
    @SerializedName("exception_content")
    private String exceptionContent;
}
