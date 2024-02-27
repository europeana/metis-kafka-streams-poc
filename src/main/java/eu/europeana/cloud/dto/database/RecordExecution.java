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
public class RecordExecution {
    @SerializedName("record_data")
    private String recordData;
    @SerializedName("execution_name")
    private String executionName;
}
