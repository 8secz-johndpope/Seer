 package net.sf.laja.cdd.state;
 
 import net.sf.laja.cdd.testgen.state.AddressState;
 import net.sf.laja.cdd.testgen.state.PersonState;
 import net.sf.laja.cdd.validator.ValidationErrors;
 import net.sf.laja.cdd.validator.Validator;
 import org.junit.Test;
 
 import java.util.Iterator;
 
 import static net.sf.laja.cdd.testgen.AddressCreator.*;
 import static net.sf.laja.cdd.testgen.HairColor.BROWN;
 import static net.sf.laja.cdd.testgen.PersonCreator.buildPerson;
 import static net.sf.laja.cdd.testgen.PersonCreator.createPerson;
 import static net.sf.laja.cdd.testgen.state.PersonState.PersonMutableState;
 import static org.hamcrest.CoreMatchers.equalTo;
 import static org.hamcrest.CoreMatchers.is;
 import static org.hamcrest.MatcherAssert.assertThat;
 
 public class PersonMutableStateTest {
 
     @Test
     public void shouldNotBeValidIfValidatingWithMissingAddress() {
         AddressState.AddressMutableState address = null;
         PersonMutableState state = buildPerson().withAddress(address).asMutableState();
 
         assertThat(state.isValid(), is(false));
     }
 
     @Test
     public void shouldReturnIsNullValidationErrors() {
         PersonMutableState mutableState = buildPerson().withName(null).asMutableState();
         ValidationErrors errors = mutableState.validate();
 
         ValidationErrors expectedErrors = ValidationErrors.builder()
                 .addIsNullError(mutableState, "name")
                 .addIsNullError(mutableState, "hairColor")
                 .build();
 
         assertThat(errors, equalTo(expectedErrors));
     }
 
     @Test
     public void invalidCollectionShouldReturnIsNullValidationErrors() {
         PersonMutableState mutableState = createPerson().name(null).hairColor(BROWN).children(
                 createPerson().name(null).hairColor(BROWN).children().defaults(),
                 createPerson().name(null).hairColor(BROWN).children().defaults()
         ).defaults().asMutableState();
 
         ValidationErrors errors = mutableState.validate();
         Iterator<ValidationErrors.ValidationError> iterator = errors.iterator();
 
         assertThat(errors.size(), is(2));
         assertThat(iterator.next(), equalTo(new ValidationErrors.ValidationError("name", "is_null", mutableState)));
         assertThat(iterator.next(), equalTo(new ValidationErrors.ValidationError("children.name", "is_null", mutableState)));
     }
 
     @Test
     public void invalidStateShouldReturnCustomValidationError() {
         PersonMutableState mutableState = createPerson().name("Carl").hairColor(BROWN).children()
                 .address(createAddress().withStreetName("First street").withCity("Stockholm"))
                 .groupedAddresses(createAddressMap(createAddressEntry("A", createAddress())))
                 .defaultListOfSetOfMapOfIntegers().asMutableState();
 
         ValidationErrors errors = mutableState.validate(new CarlCanNotLiveInStockholmValidator());
 
         ValidationErrors expectedErrors = ValidationErrors.builder()
                 .addError(mutableState, "address", "carl_can_not_live_in_stockholm").build();
 
         assertThat(errors, equalTo(expectedErrors));
     }
 
     static class CarlCanNotLiveInStockholmValidator implements Validator<PersonState.PersonMutableState> {
 
         public void validate(Object rootElement, PersonMutableState state, String parent, String attribute, ValidationErrors.Builder errors) {
             if ("Carl".equals(state.name) && state.address != null && "Stockholm".equals(state.address.city)) {
                 errors.addError("address", "carl_can_not_live_in_stockholm", rootElement, parent);
             }
         }
     }
 }
