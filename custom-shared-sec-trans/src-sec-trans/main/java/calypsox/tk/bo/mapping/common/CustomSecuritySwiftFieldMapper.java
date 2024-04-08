package calypsox.tk.bo.mapping.common;

import calypsox.tk.swift.formatter.CalypsoAppIdentifier;

import java.util.Optional;

/**
 * @author aalonsop
 */
public interface CustomSecuritySwiftFieldMapper {

    /**
     * @param relaValue
     * @return Message related reference without CYO's identifier
     */
    public default String parseRelatedMessageReference(String relaValue) {
        return Optional.ofNullable(relaValue)
                .map(rela -> {
                    String identifier = CalypsoAppIdentifier._5PSA.toString().replace("_", "");
                    return rela.startsWith(identifier)
                            ? rela.replace(identifier, "")
                            : rela.replace(CalypsoAppIdentifier.CYO.toString(), "");
                })
                .orElse("");
    }
}
