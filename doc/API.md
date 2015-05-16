---
title: Freecoin API
---

# Introduction

The Freecoin API is not stateless, but stores the API KEY in a session cookie or `apikey` POST field for authenticating.

The API KEY is obtained on wallet creation or restore, we may refer to it simply as "key". All other fields are highlighted `this way`.

All GET operations presume to have the key in place as session cookie. If the global parameter `external-webapp-url` is set, then will POST the result to the configured url including the field `api-call`, `nick` and any other field specified.

All failing operations may also return an error in the form:

    {
        "error": "Error description"
        "class": "fatal" or "warning"
        "call": "/api/call"
    }

As this is a work in progress, each section reports API completion with TODO and DONE sections.

# Wallet

 - TODO
   - GET /wallet
   - POST /wallet
   - POST /wallet/create
   - GET /wallet/create/:confirmation
   - POST /wallet/recover
   
## GET /wallet

Open the wallet. Returns `balance`.

## POST /wallet

Same as get, returns a JSON structure as:

    {
        "nick": "nickname"
        "email": "email@address"
        "balance": float
        "created": date of wallet creation
    }

## POST /wallet/create

Check for duplicate nick and email in database, if available returns success with a `confirmation` hash to be used in a subsequent call to `/wallet/create/:confirmation`

Field: `nick`, `email`

#### example request

    curl http://nxt.dyne.org/wallet/create \
        -F "nick=luther" \
        -F "email=luther@dyne.org"

#### example response

    {
        "nick": "luther"
        "email": "luther@dyne.org"
        "hash": "sL70fkl82fgtcoZY"
    }

## GET /wallet/create/:confirmation

Check if the `confirmation` code is correct, then proceed creating the wallet. Returns the full wallet structure (design in progress).

## POST /wallet/recover

Check for `email` in database, if found read and use `ah` and `al` pins to restore the wallet

#### example request

    curl https://fxc.dyne.org/wallet/recover \
        -F "email=luther@dyne.org" \
        -F "ah=ASE33DEF5" \
        -F "al=GT674GOP4"

#### example response

Will setup an url to have session cookies on a browser and return success.

    {
        "apikey": "6mHAzKHuLclviVAm"
    }

# Sending

Sending among participants is optional and depends from the monetary system setting by the issuing organization. If allowed the send API will be available for a "free market" among participants, making them able to transfer amounts among themselves.

 - TODO
   - GET /send/:participant/:amount
   - POST /send
   
## GET /send/:participant/:amount

Send `amount` to `participant`, return a human readable response with a `txid` linking to the transaction, the `amount` and the recipient `participant`.

## POST /send

Send `amount` to `participant`

#### example request

    curl https://fxc.dyne.org/send \
        -F "apikey=zvYiabC56wxta9B2" \
        -F "amount=100" \
        -F "participant=luther"

#### example response

    {
        "amount": 100
        "recipient": "luther"
        "txid": "z5oJGVVKiWTLizpT"
    }

# Stashes

A stash is a pre-defined amount that can be transferred off-line using a secret code or image (qrcode)

Using stashes is possible to make intuitive a POS transaction:

1. participant scans the price to be paid, link to /stash/create url
2. wallet respond with a QRcode that is a /stash/claim/:stash-id url
3. participant offers the QRcode to the vendor for scanning
4. vendor scans the /stash/claim url and confirms payment

- TODO
  - API design
  - GET /stash/create/:amount
  - POST /stash/create
  - GET /stash/claim/:stash-id
  - POST /stash/claim
  
## GET /stash/create/:amount

Create a signed stash of `amount` ready to be given, returns human readable stash-id and QR image.

## POST /stash/create

Same as GET, but with POST field `amount`

## GET /stash/claim/:stash-id

Claim a stash of coins by url (can also be a direct link from qrcode)

## POST /stash/claim

Same as GET, but with POST field `stash-id`



# Clearing house

Clearing house must be interfaced with democratic decision making tools

Objective8, YourPriorities, DemocracyOS, Mutual_Credit

## TODO



# Vendor relations

Vendor value transactions must be customized ad-hoc for pilots: transport companies, tax departments, service providers.

- TODO
  - API design
  - GET /vendor/send/:amount/:vendor
  - GET /vendor/send/:amount/:vendor/:participant
  - POST /vendor/send
  - vendor plugins


## GET /vendor/send/:amount/:vendor

Send `amount` to `vendor` in exchange for service credit for self, returns human readable message, all accepted fields and `txid` linking to transaction.

## GET /vendor/send/:amount/:vendor/:participant

Send `amount` to `vendor` in exchange of service credit for another `participant`, returns human readable message, all accepted fields plus a `txid` linking to transaction.

## POST /vendor/send

Same as GET operations, with POST fields `amount` and `vendor`, optional field `participant`.

# Key encryption scheme

Authentication in Freecoin is based on Shamir's Secret Sharing encryption scheme (SSSS).

We envision a scenario where we have three actors:

- Organization (minting and administering the currency)
- Participant (single individuals using the currency)
- Vendor (third-party providing value for the currency)

We want a situation in which one of these actors alone is not able to access the wallet, but at least the consent of two actors is necessary, where the most common case will be:

- Organization & Participant (to view funds, send and receive)
- Organization & Vendor (to redeem currency)

The last case, Participant & Vendor, will likely not occur in normal situations, but is a warranty that the funds will exist even if the Organization disappears.

## FXC implementation

This implementation is called "FXC protocol" and this is version 1.

The FXC passphrase is created by two random sequences in the form `FXC1_random_FXC_random_0`

The two random parts are then split in 9 slices with quorum 5 using SSSS, the NXT passphrase is deleted and only the SSSS slices can be used to retrieve it.

Slices form a 2x9 array of SSSS keys which are vertically associated.

Example:


    P = Participant (single wallet)
    O = Organization (online clearing house server)
    B = Backup (cold storage, third party)

         :HI        :LO
    FXC1_random_FXC_random_1 - P     } cookie

    FXC1_random_FXC_random_2 - P     } participant's backup
    FXC1_random_FXC_random_3 - P     } participant's backup

    FXC1_random_FXC_random_4 - O & P } participant's backup

    FXC1_random_FXC_random_5 - O
    FXC1_random_FXC_random_6 - O

    FXC1_random_FXC_random_7 - O & B } backup organization

    FXC1_random_FXC_random_8 - B     } backup organization
    FXC1_random_FXC_random_9 - B     } backup organization


The random is a long integer encoded using hashid using an alphabet that is fine tuned to not include any ambiguous character and eventually be communicated between humans without the use of computers. The salt used to hashid encode is semi-secret and is communicated to all parties.

## Tokens

Every authenticated session should contain this information:

Session:

    {
        uuid:  string
        slice: string
    }

These values are either found as private cookies or generated by a signup process. The implementation is in secretshare.clj and auth.clj (WIP)

## Backup

The backup of authentication can be made saving users email and/or providing a QR code for print and cold storage

The QR will contain:

    {
        uuid: string
        slice-1: string
        slice-2: string
        slice-3: string
    }

## Authentication flow

P = participant
O = Organization

`O <- P :` if valid cookie *login* else propose *signin* or *recover*

Signin:

`O -> P :` create NXT pass, slice it and delete the original. Save slices in db and backup, return apikey.

Login:

`O -> P :` get the apikey and retrieve slices from db, shamir-combine to rebuild the NXT pass, access NXT.

Recover:

`O <- P :` ask email, retrieve slices from db, verify apikey pins, return apikey




# More TODO

Progress using http://swagger.io

Ring-swagger https://github.com/metosin/ring-swagger

Defining schema using Prismatic https://github.com/Prismatic/schema
