package calypsox.repoccp.reader;

import calypsox.repoccp.model.ReconCCP;
import calypsox.repoccp.model.ReconCCPTrade;

import java.util.List;

public interface ReconCCPReader {

    List<ReconCCP> read(String filePath);
}
