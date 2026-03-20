package j2ee_backend.nhom05.dto.pcbuilder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PcBuildSelection {
    private Long cpuId;
    private Long mainboardId;
    private Long ramId;
    private Long gpuId;
    private Long storageId;
    private Long psuId;
    private Long caseId;
    private Long coolingId;
}
