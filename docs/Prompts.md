# Prompts

## initial state

 - one controller (customerController)
 - full controller/service/repo
 - no CODE_GUIDE or anything

## First Prompt

### Prompt 
> My project currenlty only handles returning details of a customer by id.
> 
> I want to add the following features:
> 
>  - in appServer.in.file, something that detects if a new "prestations.csv" file has been found in the 'input' folder ; this folder should be defined in applicaiton.properties.
>    - if present, it should be read, mapped to Prestation documents.
>    - if not yet present in the mongo database, they should be stored.
>    - if already present in DB, update the existing document
>  - new endpoint allowing paginated search of customer, with a query seach on the name (name of returned customers should contain an input given in the search API parameters)
>  - new endpoint allowing creation of a customer
>  - new endpoint allowing addition of a contract to an existing customer.
> 
>    - The contract should include a list of sold prestations and a type. If the type is PERMANENT, no end date should be set. If FREE_TRIAL, the endate should not be later than one month after startDate
>    - No contracts should overlap
>    - all soldPrestations should be linked to Prestation found in mongo
>  - new controller that allows :
>    - add an activty (prestation done) for a customer. An activityService should check that : - a contract is ongoing for the activity date - includes the prestation of the activity (identified by "saleSystemId") in the soldPrestation of the ongoing contract - the total activity for this prestation does not exceed what is included for the ongoing in all soldprestations of this salesSystemId"
>    - get the list of all activities done for a contract
> 
>  - every hour, a "report_XXXX.csv" file should be written in the 'output' folder (defined in application.properties) ; here XXXX should be replaced by the writing timestamp.
>    - this file should contain a line by ongoing contract, listing the contract id, the sum of billed_amount_euris of all activities done, and the remaining balance left on the contrat
>    - The file writing logic should be in appserver.out.file

### Output analysis 

  - What did it do ?
    - 772 lines added, 12 lines removed
  - Does it compile ?
    - NO : one instance of `customerId.value()` should have been `customerId.id()`
  - Does it run (once obvious compilation errors are fixed) ?
    - Yes
  - Is is good code ?
    - Yes.
      - Tier architecture is somehow respected. Controllers call services, services calls repo.
      - Without instruction, the output code respect my conventions
        - Entities next to repo, one package per entity type/repo)
      - The tier architecture around files is not what I wanted, but I did not specify exactly what I expected.
  - Small fumble around customerId / mongo ObjectId (do not expose ObjectId in DTO, use a string instead)
  - Path properties were not working, and injected String instead of Path
  - Error on reading the input CSV file : the move is done before closing the file, so we have an IOException
  - **<span style="color:#9E5">But it made sure the input file is moved and archived after reading, so it is not read twice, without any prompting from my part.</span>**. **GOOD BOT**
    