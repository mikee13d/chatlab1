# README · Chat-Lab

A Spring Boot application that acts as middleware between the user and an external LLM (via OpenRouter).
The application manages personalities, conversation memory, and forwards requests to the AI model.

## Requirements

Java 21+
Maven 
An OpenRouter account and an API key

## Get Your API Key

Go to OpenRouter Keys Page
Log in or create an account
Click Create Key
Copy the key 

## Set the API Key
The application reads the API key from the API_KEY environment variable.

Alternativ 1:
Windows (PowerShell):
powershell$env:API_KEY="insert_your_key_here"
.\mvnw spring-boot:run


Windows (CMD):
cmdset API_KEY=insert_your_key_here
mvnw spring-boot:run



Mac/Linux:
bashexport API_KEY=insert_your_key_here
./mvnw spring-boot:run

Alternativ 2:

Environment Variable in IntelliJ IDEA
Open Run → Edit Configurations
Select your Spring Boot configuration
Click Modify options → Environment variables
Add: API_KEY=$insert_your_key_here
Click OK and run the application

## Open the application

Open the browser and go to:  http://localhost:8080


## Available Personalities

coder	Senior Java developer with code examples

pirate	Pirate who speaks in pirate slang

poet	Romantic poet who replies in rhymes

other	Regular helpful assistant

## Run tests

Powershell
.\mvnw test