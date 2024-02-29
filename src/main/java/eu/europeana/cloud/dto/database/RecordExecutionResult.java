package eu.europeana.cloud.dto.database;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@AllArgsConstructor
@ToString
public class RecordExecutionResult extends RecordExecutionProduct {
    @SerializedName("record_result_data")
    private String recordResultData;
    @SerializedName("execution_name")
    private String executionName;
}
