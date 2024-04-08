package calypsox.tk.anacredit.api.attributes;

/**
 * Specialities on defined fields
 * NONE - NONE
 * SIGNAl_PLUS -  Force always positive values
 * SIGNAL_MINUS - Force Always Negative Values
 * FOECE_NULL - Force spaces even if its numeric
 */
public enum DataAttribute {
    NONE,
    SIGNAL_PLUS,
    SIGNAL_MINUS,
    FORCE_NULL,
}
