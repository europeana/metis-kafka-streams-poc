package eu.europeana.cloud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Builder
@Getter
@Setter
public class ErrorInformation {
    private String errorMessage;
    private String additionalInformation;

}
