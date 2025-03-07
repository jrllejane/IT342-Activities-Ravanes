package com.ravanes.google.oauth.Service;

import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleContactsService {

    private final OAuth2AuthorizedClientService authorizedClientService;

    public GoogleContactsService(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    // Retrieve access token from Spring Security context
    private String getAccessToken() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(),
                    oauthToken.getName()
            );
            if (client != null && client.getAccessToken() != null) {
                return client.getAccessToken().getTokenValue();
            }
        }
        throw new RuntimeException("OAuth2 authentication failed!");
    }

    // Create PeopleService instance
    private PeopleService createPeopleService() {
        return new PeopleService.Builder(
                new com.google.api.client.http.javanet.NetHttpTransport(),
                new com.google.api.client.json.gson.GsonFactory(),
                request -> request.getHeaders().setAuthorization("Bearer " + getAccessToken())
        ).setApplicationName("Google Contacts App").build();
    }

    // Fetch contacts
    public List<Person> getContacts() throws IOException {
        PeopleService peopleService = createPeopleService();
        ListConnectionsResponse response = peopleService.people().connections()
                .list("people/me")
                .setPersonFields("names,emailAddresses,phoneNumbers,photos")
                .execute();

        return response.getConnections() != null ? response.getConnections() : new ArrayList<>();
    }

    // Create a new contact
    public Person createContact(String givenName, String familyName, List<String> emails, List<String> phoneNumbers) throws IOException {
        PeopleService peopleService = createPeopleService();
        Person newPerson = new Person();

        newPerson.setNames(List.of(new Name().setGivenName(givenName).setFamilyName(familyName)));

        // Handle multiple emails
        if (emails != null && !emails.isEmpty()) {
            List<EmailAddress> emailList = new ArrayList<>();
            for (String email : emails) {
                emailList.add(new EmailAddress().setValue(email));
            }
            newPerson.setEmailAddresses(emailList);
        }

        // Handle multiple phone numbers
        if (phoneNumbers != null && !phoneNumbers.isEmpty()) {
            List<PhoneNumber> phoneList = new ArrayList<>();
            for (String phoneNumber : phoneNumbers) {
                phoneList.add(new PhoneNumber().setValue(phoneNumber));
            }
            newPerson.setPhoneNumbers(phoneList);
        }

        return peopleService.people().createContact(newPerson).execute();
    }

    // Update an existing contact
    public void updateContact(String resourceName, String givenName, String familyName, List<String> emails, List<String> phoneNumbers) throws IOException {
        PeopleService peopleService = createPeopleService();
        Person existingContact = peopleService.people().get(resourceName)
                .setPersonFields("names,emailAddresses,phoneNumbers")
                .execute();

        Person updatedContact = new Person()
                .setEtag(existingContact.getEtag())
                .setNames(List.of(new Name().setGivenName(givenName).setFamilyName(familyName)));

        // Handle multiple emails
        if (emails != null && !emails.isEmpty()) {
            List<EmailAddress> emailList = new ArrayList<>();
            for (String email : emails) {
                emailList.add(new EmailAddress().setValue(email));
            }
            updatedContact.setEmailAddresses(emailList);
        }

        // Handle multiple phone numbers
        if (phoneNumbers != null && !phoneNumbers.isEmpty()) {
            List<PhoneNumber> phoneList = new ArrayList<>();
            for (String phoneNumber : phoneNumbers) {
                phoneList.add(new PhoneNumber().setValue(phoneNumber));
            }
            updatedContact.setPhoneNumbers(phoneList);
        }

        peopleService.people().updateContact(resourceName, updatedContact)
                .setUpdatePersonFields("names,emailAddresses,phoneNumbers")
                .execute();
    }

    // Delete a contact
    public void deleteContact(String resourceName) throws IOException {
        PeopleService peopleService = createPeopleService();
        peopleService.people().deleteContact(resourceName).execute();
    }
}
