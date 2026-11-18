package se.jtutala.handlaggning.galdenar;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/// Lär JPA att spara `Personnummer` som en vanlig textkolumn.
@Converter(autoApply = true)
class PersonnummerConverter implements AttributeConverter<Personnummer, String> {

    @Override
    public String convertToDatabaseColumn(Personnummer personnummer) {
        return personnummer.varde();
    }

    @Override
    public Personnummer convertToEntityAttribute(String varde) {
        return new Personnummer(varde);
    }
}
