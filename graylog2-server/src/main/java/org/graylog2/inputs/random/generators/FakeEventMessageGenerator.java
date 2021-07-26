package org.graylog2.inputs.random.generators;

import com.google.common.collect.ImmutableList;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.joda.time.DateTime;

import java.util.Optional;
import java.util.Random;

public class FakeEventMessageGenerator extends FakeMessageGenerator {
    private static final ImmutableList<Category> CATEGORIES = ImmutableList.of(
            new Category("100000", GeneratorState.CategoryName.AUTHENTICATION, "logon", "logon", 8),
            new Category("100001", GeneratorState.CategoryName.AUTHENTICATION, "logon", "logon success", 10),
            new Category("100002", GeneratorState.CategoryName.AUTHENTICATION, "logon", "logon failure", 2),
            new Category("100003", GeneratorState.CategoryName.AUTHENTICATION, "logon", "logon with alternate credentials", 1),
            new Category("100004", GeneratorState.CategoryName.AUTHENTICATION, "logon", "session reconnect", 4),
            new Category("102500", GeneratorState.CategoryName.AUTHENTICATION, "logoff", "logoff", 4),
            new Category("102501", GeneratorState.CategoryName.AUTHENTICATION, "logoff", "session disconnect", 4),
            new Category("160500", GeneratorState.CategoryName.ENDPOINT, "ports", "port open", 2),
            new Category("160501", GeneratorState.CategoryName.ENDPOINT, "ports", "port closed", 2),
            new Category("160502", GeneratorState.CategoryName.ENDPOINT, "ports", "open ports", 2)
    );

    private static final ImmutableList<User> USERS = ImmutableList.of(
            new User("rand", "1001", "black-tower", 5),
            new User("mat", "1002",  "red-hand", 3),
            new User("perrin", "1003",  "edmonds-field", 3),
            new User("egwene", "1004",  "white-tower", 3),
            new User("nynaeve", "1005",  "white-tower", 3),
            new User("moraine", "1006",  "white-tower", 4),
            new User("elayne", "1006",  "caemlyn", 4)
    );

    private static final ImmutableList<Application> APPLICATIONS = ImmutableList.of(
            new Application("LDAP", 10),
            new Application("RADIUS", 4),
            new Application("Active Directory", 8)
    );

    private static final ImmutableList<Reference> REFERENCES = ImmutableList.of(
            new Reference("edmonds-field.acme.org", 10),
            new Reference("tar-valon.acme.org", 10),
            new Reference("caemlyn.acme.org", 10),
            new Reference("tear.acme.org", 10),
            new Reference("illian.acme.org", 10),
            new Reference("mayene.acme.org", 10),
            new Reference("black-tower.acme.org", 10),
            new Reference("four-kings.acme.org", 10)
    );

    private static final ImmutableList<EventSource> EVENT_SOURCES = ImmutableList.of(
            new EventSource("firefox", 10),
            new EventSource("chrome", 8),
            new EventSource("irssi", 5),
            new EventSource("thunderbrid", 2)
    );

    public FakeEventMessageGenerator(Configuration configuration) {
        super(configuration);
    }

    @Override
    public GeneratorState generateState() {
        final GeneratorState generatorState = new GeneratorState();
        final int successProb = RANDOM.nextInt(100);

        generatorState.isSuccessful = successProb < 97;
        generatorState.msgSequenceNr = msgSequenceNumber++;
        generatorState.source = source;
        generatorState.eventTypeCode = ((Category) getWeighted(CATEGORIES)).gl2EventTypeCode;
        generatorState.userName = ((User) getWeighted(USERS)).userName;
        generatorState.applicationName = ((Application) getWeighted(APPLICATIONS)).applicationName;
        generatorState.destinationRef = ((Reference) getWeighted(REFERENCES)).referenceName;
        generatorState.sourceRef = ((Reference) getWeighted(REFERENCES)).referenceName;
        generatorState.eventSource = ((EventSource) getWeighted(EVENT_SOURCES)).eventSource;
        generatorState.country = ((Country) getWeighted(COUNTRIES)).country;

        return generatorState;
    }

    private static Message createMessage(GeneratorState state, Category category, String shortMessage) {
        final Message msg = new Message(shortMessage, state.source, Tools.nowUTC());
        final Country country = COUNTRIES.stream().filter(c -> c.country.equals(state.country)).findFirst().orElseThrow(() -> new RuntimeException("Could not find country: " + state.country));
        msg.addField("sequence_nr", state.msgSequenceNr);
        msg.addField("gl2_event_type_code", category.gl2EventTypeCode);
        msg.addField("gl2_event_category", category.gl2EventCategory);
        msg.addField("gl2_event_subcategory", category.gl2EventSubcategory);
        msg.addField("gl2_event_type", category.gl2EventType);
        msg.addField("country", country.country);
        msg.addField("city", country.capital);
        msg.addField("geolocation", country.geolocation);

        return msg;
    }

    public static Message generateMessage(GeneratorState state) {
        final Category category = CATEGORIES.stream().filter(c -> c.gl2EventTypeCode.equals(state.eventTypeCode)).findFirst().orElseThrow(() -> new RuntimeException("Could not find category"));

        switch (category.gl2EventCategory) {
            case AUTHENTICATION: return simulateAuthentication(state, RANDOM);
            case ENDPOINT: return simulateEndpoint(state, RANDOM);
            default: throw new RuntimeException("Unknown Category" + category.gl2EventCategory);
        }
    }

    public static Message simulateEndpoint(GeneratorState state, Random rand) {
        final String shortMessage = getShortMessage(state);
        final Category category = CATEGORIES.stream().filter(c -> c.gl2EventTypeCode.equals(state.eventTypeCode)).findFirst().orElseThrow(() -> new RuntimeException("Could not find category"));
        final Message msg = createMessage(state, category, shortMessage);

        msg.addField("event_source", state.eventSource);
        msg.addField("user_name", state.userName);
        msg.addField("process_id", rand.nextInt(10000));
        msg.addField("process_command_line", "/usr/bin/" + state.eventSource);

        return msg;
    }

    public static String getShortMessage(GeneratorState state) {
        switch (state.eventTypeCode) {
            case "100000": return DateTime.now() + ": User " + state.userName + " is logging on " + state.destinationRef;
            case "100001": return DateTime.now() + ": User " + state.userName + " is successfully logging on " + state.destinationRef;
            case "100002": return DateTime.now() + ": User " + state.userName + " failed to logon " + state.destinationRef;
            case "100003": return DateTime.now() + ": User " + state.userName + " is logging on with alternate credentials " + state.destinationRef;
            case "100004": return DateTime.now() + ": User " + state.userName + " the session was reconnected to " + state.destinationRef;
            case "102500": return DateTime.now() + ": User " + state.userName + " has logged of from " + state.destinationRef;
            case "102501": return DateTime.now() + ": User " + state.userName + " was disconnected from" + state.destinationRef;
            case "160500": return DateTime.now() + ": User " + state.userName + " opened port for " + state.eventSource;
            case "160501": return DateTime.now() + ": User " + state.userName + " closed port for " + state.eventSource;
            case "160502": return DateTime.now() + ": User " + state.userName + " has open ports for " + state.eventSource;
            default: return "unknown event type";
        }
    }

    public static Message authenticationLogonFields(Message msg, GeneratorState state) {
        final Optional<User> user = USERS.stream().filter(u -> u.userName.equals(state.userName)).findFirst();
        msg.addField("application_name", state.applicationName);
        msg.addField("destination_reference", state.destinationRef);
        msg.addField("source_reference", state.sourceRef);
        msg.addField("event_outcome", state.isSuccessful);
        user.ifPresent(u -> msg.addField("user_domain", u.userDomain));
        msg.addField("user_name", state.userName);

        return msg;
    }

    public static Message authenticationLogoffFields(Message msg, GeneratorState state) {
        final Optional<User> user = USERS.stream().filter(u -> u.userName.equals(state.userName)).findFirst();
        msg.addField("application_name", state.applicationName);
        msg.addField("source_reference", state.sourceRef);
        user.ifPresent(u -> msg.addField("user_domain", u.userDomain));
        msg.addField("user_name", state.userName);

        return msg;
    }

    public static Message simulateAuthentication(GeneratorState state, Random rand) {
        final String shortMessage = getShortMessage(state);
        final Category category = CATEGORIES.stream().filter(c -> c.gl2EventTypeCode.equals(state.eventTypeCode)).findFirst().orElseThrow(() -> new RuntimeException("Could not find category"));
        final Message msg = createMessage(state, category, shortMessage);

        switch (category.gl2EventSubcategory) {
            case "logon": return authenticationLogonFields(msg, state);
            case "logoff": return authenticationLogoffFields(msg, state);
            default: return msg;
        }
    }

    public static class GeneratorState extends FakeMessageGenerator.GeneratorState {
        public long msgSequenceNr;
        public boolean isSuccessful;
        public String source;
        public String eventTypeCode;
        public String userName;
        public String applicationName;
        public String destinationRef;
        public String sourceRef;
        public String eventSource;
        public String country;

        public enum CategoryName {
            AUTHENTICATION, ENDPOINT,
        }
    }

    private static class Application extends Weighted {
        private final String applicationName;

        private Application(String applicationName, int weight) {
            super(weight);

            this.applicationName = applicationName;
        }

        public String getApplicationName() {
            return applicationName;
        }
    }

    private static class EventSource extends Weighted {
        private final String eventSource;

        private EventSource(String eventSource, int weight) {
            super(weight);

            this.eventSource = eventSource;
        }

        public String getApplicationName() {
            return eventSource;
        }
    }

    private static class Reference extends Weighted {
        private final String referenceName;

        private Reference(String refernceName, int weight) {
            super(weight);

            this.referenceName = refernceName;
        }

        public String getReferenceName() {
            return referenceName;
        }
    }

    private static class User extends Weighted {
        private final String userName;
        private final String userId;
        private final String userDomain;


        private User(String userName, String userId, String userDomain, int weight) {
            super(weight);

            this.userName = userName;
            this.userId = userId;
            this.userDomain = userDomain;
        }

        public String getUserName() {
            return userName;
        }

        public String getUserId() {
            return userId;
        }

        public String getUserDomain() {
            return userDomain;
        }
    }

    private static class Category extends Weighted {
       private final String gl2EventTypeCode;
       private final GeneratorState.CategoryName gl2EventCategory;
       private final String gl2EventSubcategory;
       private final String gl2EventType;

        public Category(String gl2EventTypeCode, GeneratorState.CategoryName gl2EventCategory, String gl2EventSubcategory, String gl2EventType, int weight) {
            super(weight);

            this.gl2EventTypeCode = gl2EventTypeCode;
            this.gl2EventCategory = gl2EventCategory;
            this.gl2EventSubcategory = gl2EventSubcategory;
            this.gl2EventType = gl2EventType;
        }

        public String getTypeCode() {
           return gl2EventTypeCode;
        }

        public GeneratorState.CategoryName getCategory() {
            return gl2EventCategory;
        }

        public String getSubcategory() {
            return gl2EventSubcategory;
        }

        public String getType() {
            return gl2EventType;
        }
    }

    private static class Country extends Weighted {
        private final String country;
        private final String capital;
        private final String geolocation;

        private Country(String country, String capital, String geolocation, int weight) {
            super(weight);

            this.country = country;
            this.capital = capital;
            this.geolocation = geolocation;
        }

        public String getGeolocation() {
            return geolocation;
        }

        public String getCapital() {
            return capital;
        }

        public String getCountry() {
            return country;
        }
    }

    final static ImmutableList<Country> COUNTRIES = ImmutableList.of(
            new Country("Afghanistan", "Kabul", "34.28, 69.11", 1),
            new Country("Albania", "Tirane", "41.18, 19.49", 1),
            new Country("Algeria", "Algiers", "36.42, 03.08", 1),
            new Country("American Samoa", "Pago Pago", "-14.16, -170.43", 1),
            new Country("Andorra", "Andorra la Vella", "42.31, 01.32", 1),
            new Country("Angola", "Luanda", "-08.50, 13.15", 1),
            new Country("Antigua and Barbuda", "West Indies", "17.20, -61.48", 1),
            new Country("Argentina", "Buenos Aires", "-36.30, -60.00", 1),
            new Country("Armenia", "Yerevan", "40.10, 44.31", 1),
            new Country("Aruba", "Oranjestad", "12.32, -70.02", 1),
            new Country("Australia", "Canberra", "-35.15, 149.08", 1),
            new Country("Austria", "Vienna", "48.12, 16.22", 1),
            new Country("Azerbaijan", "Baku", "40.29, 49.56", 1),
            new Country("Bahamas", "Nassau", "25.05, -77.20", 1),
            new Country("Bahrain", "Manama", "26.10, 50.30", 1),
            new Country("Bangladesh", "Dhaka", "23.43, 90.26", 1),
            new Country("Barbados", "Bridgetown", "13.05, -59.30", 1),
            new Country("Belarus", "Minsk", "53.52, 27.30", 1),
            new Country("Belgium", "Brussels", "50.51, 04.21", 1),
            new Country("Belize", "Belmopan", "17.18, -88.30", 1),
            new Country("Benin", "Porto Novo (constitutional, 1) / Cotonou (seat of government, 1)", "06.23, 02.42", 1),
            new Country("Bhutan", "Thimphu", "27.31, 89.45", 1),
            new Country("Bolivia", "La Paz (administrative, 1) / Sucre (legislative, 1)", "-16.20, -68.10", 1),
            new Country("Bosnia and Herzegovina", "Sarajevo", "43.52, 18.26", 1),
            new Country("Botswana", "Gaborone", "-24.45, 25.57", 1),
            new Country("Brazil", "Brasilia", "-15.47, -47.55", 1),
            new Country("British Virgin Islands", "Road Town", "18.27, -64.37", 1),
            new Country("Brunei Darussalam", "Bandar Seri Begawan", "04.52, 115.00", 1),
            new Country("Bulgaria", "Sofia", "42.45, 23.20", 1),
            new Country("Burkina Faso", "Ouagadougou", "12.15, -01.30", 1),
            new Country("Burundi", "Bujumbura", "-03.16, 29.18", 1),
            new Country("Cambodia", "Phnom Penh", "11.33, 104.55", 1),
            new Country("Cameroon", "Yaounde", "03.50, 11.35", 1),
            new Country("Canada", "Ottawa", "45.27, -75.42", 1),
            new Country("Cape Verde", "Praia", "15.02, -23.34", 1),
            new Country("Cayman Islands", "George Town", "19.20, -81.24", 1),
            new Country("Central African Republic", "Bangui", "04.23, 18.35", 1),
            new Country("Chad", "N'Djamena", "12.10, 14.59", 1),
            new Country("Chile", "Santiago", "-33.24, -70.40", 1),
            new Country("China", "Beijing", "39.55, 116.20", 1),
            new Country("Colombia", "Bogota", "04.34, -74.00", 1),
            new Country("Comros", "Moroni", "-11.40, 43.16", 1),
            new Country("Congo", "Brazzaville", "-04.09, 15.12", 1),
            new Country("Costa Rica", "San Jose", "09.55, -84.02", 1),
            new Country("Cote d'Ivoire", "Yamoussoukro", "06.49, -05.17", 1),
            new Country("Croatia", "Zagreb", "45.50, 15.58", 1),
            new Country("Cuba", "Havana", "23.08, -82.22", 1),
            new Country("Cyprus", "Nicosia", "35.10, 33.25", 1),
            new Country("Czech Republic", "Prague", "50.05, 14.22", 1),
            new Country("Democratic Republic of the Congo", "Kinshasa", "-04.20, 15.15", 1),
            new Country("Denmark", "Copenhagen", "55.41, 12.34", 1),
            new Country("Djibouti", "Djibouti", "11.08, 42.20", 1),
            new Country("Dominica", "Roseau", "15.20, -61.24", 1),
            new Country("Dominica Republic", "Santo Domingo", "18.30, -69.59", 1),
            new Country("East Timor", "Dili", "-08.29, 125.34", 1),
            new Country("Ecuador", "Quito", "-00.15, -78.35", 1),
            new Country("Egypt", "Cairo", "30.01, 31.14", 1),
            new Country("El Salvador", "San Salvador", "13.40, -89.10", 1),
            new Country("Equatorial Guinea", "Malabo", "03.45, 08.50", 1),
            new Country("Eritrea", "Asmara", "15.19, 38.55", 1),
            new Country("Estonia", "Tallinn", "59.22, 24.48", 1),
            new Country("Ethiopia", "Addis Ababa", "09.02, 38.42", 1),
            new Country("Falkland Islands (Malvinas, 1)", "Stanley", "-51.40, -59.51", 1),
            new Country("Faroe Islands", "Torshavn", "62.05, -06.56", 1),
            new Country("Fiji", "Suva", "-18.06, 178.30", 1),
            new Country("Finland", "Helsinki", "60.15, 25.03", 1),
            new Country("France", "Paris", "48.50, 02.20", 10),
            new Country("French Guiana", "Cayenne", "05.05, -52.18", 1),
            new Country("French Polynesia", "Papeete", "-17.32, -149.34", 1),
            new Country("Gabon", "Libreville", "00.25, 09.26", 1),
            new Country("Gambia", "Banjul", "13.28, -16.40", 1),
            new Country("Georgia", "T'bilisi", "41.43, 44.50", 1),
            new Country("Germany", "Berlin", "52.30, 13.25", 10),
            new Country("Ghana", "Accra", "05.35, -00.06", 1),
            new Country("Greece", "Athens", "37.58, 23.46", 1),
            new Country("Greenland", "Nuuk", "64.10, -51.35", 1),
            new Country("Guadeloupe", "Basse-Terre", "16.00, -61.44", 1),
            new Country("Guatemala", "Guatemala", "14.40, -90.22", 1),
            new Country("Guernsey", "St. Peter Port", "49.26, -02.33", 1),
            new Country("Guinea", "Conakry", "09.29, -13.49", 1),
            new Country("Guinea-Bissau", "Bissau", "11.45, -15.45", 1),
            new Country("Guyana", "Georgetown", "06.50, -58.12", 1),
            new Country("Haiti", "Port-au-Prince", "18.40, -72.20", 1),
            new Country("Heard Island and McDonald Islands", " ", "-53.00, 74.00", 1),
            new Country("Honduras", "Tegucigalpa", "14.05, -87.14", 1),
            new Country("Hungary", "Budapest", "47.29, 19.05", 1),
            new Country("Iceland", "Reykjavik", "64.10, -21.57", 1),
            new Country("India", "New Delhi", "28.37, 77.13", 10),
            new Country("Indonesia", "Jakarta", "-06.09, 106.49", 1),
            new Country("Iran (Islamic Republic of, 1)", "Tehran", "35.44, 51.30", 1),
            new Country("Iraq", "Baghdad", "33.20, 44.30", 1),
            new Country("Ireland", "Dublin", "53.21, -06.15", 1),
            new Country("Israel", "Jerusalem", "31.71, -35.10", 1),
            new Country("Italy", "Rome", "41.54, 12.29", 1),
            new Country("Jamaica", "Kingston", "18.00, -76.50", 1),
            new Country("Jordan", "Amman", "31.57, 35.52", 1),
            new Country("Kazakhstan", "Astana", "51.10, 71.30", 1),
            new Country("Kenya", "Nairobi", "-01.17, 36.48", 1),
            new Country("Kiribati", "Tarawa", "01.30, 173.00", 1),
            new Country("Kuwait", "Kuwait", "29.30, 48.00", 1),
            new Country("Kyrgyzstan", "Bishkek", "42.54, 74.46", 1),
            new Country("Lao People's Democratic Republic", "Vientiane", "17.58, 102.36", 1),
            new Country("Latvia", "Riga", "56.53, 24.08", 1),
            new Country("Lebanon", "Beirut", "33.53, 35.31", 1),
            new Country("Lesotho", "Maseru", "-29.18, 27.30", 1),
            new Country("Liberia", "Monrovia", "06.18, -10.47", 1),
            new Country("Libyan Arab Jamahiriya", "Tripoli", "32.49, 13.07", 1),
            new Country("Liechtenstein", "Vaduz", "47.08, 09.31", 1),
            new Country("Lithuania", "Vilnius", "54.38, 25.19", 1),
            new Country("Luxembourg", "Luxembourg", "49.37, 06.09", 1),
            new Country("Macao, China", "Macau", "22.12, 113.33", 1),
            new Country("Madagascar", "Antananarivo", "-18.55, 47.31", 1),
            new Country("Macedonia (Former Yugoslav Republic, 1)", "Skopje", "42.01, 21.26", 1),
            new Country("Malawi", "Lilongwe", "-14.00, 33.48", 1),
            new Country("Malaysia", "Kuala Lumpur", "03.09, 101.41", 1),
            new Country("Maldives", "Male", "04.00, 73.28", 1),
            new Country("Mali", "Bamako", "12.34, -07.55", 1),
            new Country("Malta", "Valletta", "35.54, 14.31", 1),
            new Country("Martinique", "Fort-de-France", "14.36, -61.02", 1),
            new Country("Mauritania", "Nouakchott", "-20.10, 57.30", 1),
            new Country("Mayotte", "Mamoudzou", "-12.48, 45.14", 1),
            new Country("Mexico", "Mexico", "19.20, -99.10", 1),
            new Country("Micronesia (Federated States of, 1)", "Palikir", "06.55, 158.09", 1),
            new Country("Moldova, Republic of", "Chisinau", "47.02, 28.50", 1),
            new Country("Mozambique", "Maputo", "-25.58, 32.32", 1),
            new Country("Myanmar", "Yangon", "16.45, 96.20", 1),
            new Country("Namibia", "Windhoek", "-22.35, 17.04", 1),
            new Country("Nepal", "Kathmandu", "27.45, 85.20", 10),
            new Country("Netherlands", "Amsterdam / The Hague (seat of Government, 1)", "52.23, 04.54", 1),
            new Country("Netherlands Antilles", "Willemstad", "12.05, -69.00", 1),
            new Country("New Caledonia", "Noumea", "-22.17, 166.30", 1),
            new Country("New Zealand", "Wellington", "-41.19, 174.46", 1),
            new Country("Nicaragua", "Managua", "12.06, -86.20", 1),
            new Country("Niger", "Niamey", "13.27, 02.06", 1),
            new Country("Nigeria", "Abuja", "09.05, 07.32", 1),
            new Country("Norfolk Island", "Kingston", "-45.20, 168.43", 1),
            new Country("North Korea", "Pyongyang", "39.09, 125.30", 1),
            new Country("Northern Mariana Islands", "Saipan", "15.12, 145.45", 1),
            new Country("Norway", "Oslo", "59.55, 10.45", 1),
            new Country("Oman", "Masqat", "23.37, 58.36", 1),
            new Country("Pakistan", "Islamabad", "33.40, 73.10", 1),
            new Country("Palau", "Koror", "07.20, 134.28", 1),
            new Country("Panama", "Panama", "09.00, -79.25", 1),
            new Country("Papua New Guinea", "Port Moresby", "-09.24, 147.08", 1),
            new Country("Paraguay", "Asuncion", "-25.10, -57.30", 1),
            new Country("Peru", "Lima", "-12.00, -77.00", 1),
            new Country("Philippines", "Manila", "14.40, 121.03", 1),
            new Country("Poland", "Warsaw", "52.13, 21.00", 1),
            new Country("Portugal", "Lisbon", "38.42, -09.10", 1),
            new Country("Puerto Rico", "San Juan", "18.28, -66.07", 1),
            new Country("Qatar", "Doha", "25.15, 51.35", 1),
            new Country("Republic of Korea", "Seoul", "37.31, 126.58", 1),
            new Country("Romania", "Bucuresti", "44.27, 26.10", 1),
            new Country("Russian Federation", "Moskva", "55.45, 37.35", 1),
            new Country("Rawanda", "Kigali", "-01.59, 30.04", 1),
            new Country("Saint Kitts and Nevis", "Basseterre", "17.17, -62.43", 1),
            new Country("Saint Lucia", "Castries", "14.02, -60.58", 1),
            new Country("Saint Pierre and Miquelon", "Saint-Pierre", "46.46, -56.12", 1),
            new Country("Saint Vincent and the Greenadines", "Kingstown", "13.10, -61.10", 1),
            new Country("Samoa", "Apia", "-13.50, -171.50", 1),
            new Country("San Marino", "San Marino", "43.55, 12.30", 1),
            new Country("Sao Tome and Principe", "Sao Tome", "00.10, 06.39", 1),
            new Country("Saudi Arabia", "Riyadh", "24.41, 46.42", 1),
            new Country("Senegal", "Dakar", "14.34, -17.29", 1),
            new Country("Sierra Leone", "Freetown", "08.30, -13.17", 1),
            new Country("Slovakia", "Bratislava", "48.10, 17.07", 1),
            new Country("Slovenia", "Ljubljana", "46.04, 14.33", 1),
            new Country("Solomon Islands", "Honiara", "-09.27, 159.57", 1),
            new Country("Somalia", "Mogadishu", "02.02, 45.25", 1),
            new Country("South Africa", "Pretoria (administrative, 1) / Cape Town (legislative, 1) / Bloemfontein (judicial, 1)", "-25.44, 28.12", 1),
            new Country("Spain", "Madrid", "40.25, -03.45", 1),
            new Country("Sudan", "Khartoum", "15.31, 32.35", 1),
            new Country("Suriname", "Paramaribo", "05.50, -55.10", 1),
            new Country("Swaziland", "Mbabane (administrative, 1)", "-26.18, 31.06", 1),
            new Country("Sweden", "Stockholm", "59.20, 18.03", 1),
            new Country("Switzerland", "Bern", "46.57, 07.28", 1),
            new Country("Syrian Arab Republic", "Damascus", "33.30, 36.18", 1),
            new Country("Tajikistan", "Dushanbe", "38.33, 68.48", 1),
            new Country("Thailand", "Bangkok", "13.45, 100.35", 1),
            new Country("Togo", "Lome", "06.09, 01.20", 1),
            new Country("Tonga", "Nuku'alofa", "-21.10, -174.00", 1),
            new Country("Tunisia", "Tunis", "36.50, 10.11", 1),
            new Country("Turkey", "Ankara", "39.57, 32.54", 1),
            new Country("Turkmenistan", "Ashgabat", "38.00, 57.50", 1),
            new Country("Tuvalu", "Funafuti", "-08.31, 179.13", 1),
            new Country("Uganda", "Kampala", "00.20, 32.30", 1),
            new Country("Ukraine", "Kiev (Russia, 1)", "50.30, 30.28", 1),
            new Country("United Arab Emirates", "Abu Dhabi", "24.28, 54.22", 1),
            new Country("United Kingdom of Great Britain and Northern Ireland", "London", "51.36, -00.05", 10),
            new Country("United Republic of Tanzania", "Dodoma", "-06.08, 35.45", 1),
            new Country("United States of America", "Washington DC", "39.91, -77.02", 10),
            new Country("United States of Virgin Islands", "Charlotte Amalie", "18.21, -64.56", 1),
            new Country("Uruguay", "Montevideo", "-34.50, -56.11", 1),
            new Country("Uzbekistan", "Tashkent", "41.20, 69.10", 1),
            new Country("Vanuatu", "Port-Vila", "-17.45, 168.18", 1),
            new Country("Venezuela", "Caracas", "10.30, -66.55", 1),
            new Country("Viet Nam", "Hanoi", "21.05, 105.55", 1),
            new Country("Yugoslavia", "Belgrade", "44.50, 20.37", 1),
            new Country("Zambia", "Lusaka", "-15.28, 28.16", 1),
            new Country("Zimbabwe", "Harare", "-17.43, 31.02", 1)
    );
}
