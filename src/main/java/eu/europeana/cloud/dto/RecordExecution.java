package eu.europeana.cloud.dto;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.*;


@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RecordExecution extends RecordExecutionProduct {
    @SerializedName("record_data")
    private String recordData;
    @SerializedName("execution_name")
    private String executionName;
    @SerializedName("execution_parameters")
    private JsonObject executionParameters;

}
