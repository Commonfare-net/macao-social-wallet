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

# Card

A card is the public facing information for every participant, it can be distributed and then used by the organization (and others participants, if allowed) to send any amount of value to the participant. It includes a qrcode that links directly to the `/send` endpoint.

 - DONE
   - GET|POST  /
   - GET  /qrcode/:name
   - GET  /find/key/value

## GET|POST /

Return a `card` including fields: `name`, `email`, `address` and a `qrcode`.

The `address` is a unique identifier of the user's public identity on the blockchain.

The qrcode is an html link to the `/qrcode` endpoint.

#### example call
POST http://localhost:8000/
Content-Type: application/json
#### example response
     {
         "QR": "<img src=\"/wallet/qrcode\" alt=\"QR\">",
         "name": "Monaca",
         "email": "monza@monastero.it",
         "address": "NXT-L5RG-P7X9-DASP-6BWFV"
     }

Note: this is inaccurate until the embedding of the image is done inside the json (TODO).

## GET /qrcode/:name

Retrieve a QRCode image that, once scanned, will redirect to the `/send` url to transfer funds to the participant.

#### example call
GET http://localhost:8000/qrcode/Monaca
Content-Type: text/html
#### example response

An PNG image with content-type `image/png` will be returned. This url can be directly used inside `<img src="">` fields.

# Wallet

A `wallet` is the private space where participants can check the funds they own.

 - DONE
   - POST     /wallet/create
   - GET      /wallet/create/:confirmation

 - TODO
   - GET/POST /wallet
   - POST     /wallet/recover

## GET/POST /wallet

Open the wallet. Returns `balance` as text/html.

#### example call
POST http://localhost:8000/wallet
Content-Type: application/json
#

#### example response

    {
        "name": "nickname"
        "email": "email@address"
        "balance": float
        "created": date of wallet creation
        "last_access": date of last access
        "last_location": location (IP) of last access
        "last_transaction": link to the last transaction occurred
    }

## POST /wallet/create

Check for duplicate name and email in database, if available returns success with a `confirmation` hash to be used in a subsequent call to `/wallet/create/:confirmation`

Field: `name`, `email`

#### example request

POST http://localhost:8000/wallet/create
Content-Type: application/json

     {
      "name": "jaromil",
      "email": "jaromil@dyne.org"
     }

#### example response

     {
      "body":
        {"email":"jaromil@dyne.org",
         "name":"jaromil",
         "_id":"D7GNYYP53E6YN"},
      "confirm":"\/wallet\/create\/D7GNYYP53E6YN"
     }

## GET /wallet/create/:confirmation

Check if the `confirmation` code is correct, then proceed creating the wallet.

Returns the full wallet structure (design in progress).

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

# Sending (TODO)

Sending among participants is optional and depends from the monetary system setting by the issuing organization. If allowed the send API will be available for a "free market" among participants, making them able to transfer amounts among themselves.

 - TODO
   - GET  /send/:participant/:amount
   - POST /send
   
## GET /send/:participant/:amount

Send `amount` to `participant`, return a human readable response with a `txid` linking to the transaction, the `amount` and the recipient `participant`.

## POST /send

Send `amount` to `participant`, return a machine readable json with the `txid`

#### example request

    curl https://fxc.dyne.org/send \
        -F "apikey=zvYiabC56wxta9B2" \
        -F "amount=100" \
        -F "participant=luther"

#### example response

    {
        "amount": 100
        "recipient": "luther"
        "transaction": the id of the transaction
    }

# Stashes (TODO)

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



# Clearing house (TODO)

Clearing house must be interfaced with democratic decision making tools

Objective8, YourPriorities, DemocracyOS, Mutual_Credit



# Vendor relations

Vendor value transactions must be customized ad-hoc for pilots: transport companies, tax departments, service providers.

- TODO
  - API design
  - GET /vendor/send/:amount/:vendor
  - GET /vendor/send/:amount/:vendor/:participant
  - POST /vendor/send
  - vendor plugins


## GET /vendor/send/:amount/:vendor

Send `amount` to `vendor` in exchange for service credit for self, returns human readable message, all accepted fields and an id linking to the `transaction`.

## GET /vendor/send/:amount/:vendor/:participant

Send `amount` to `vendor` in exchange of service credit for another `participant`, returns human readable message, all accepted fields plus an id linking to the `transaction`.

## POST /vendor/send

Same as GET operations, with POST fields `amount` and `vendor`, optional field `participant`.


# Transparent bridge to NXT API

If activated, there is a transparent bridge to most NXT API calls via `GET` on the `/nxt/` url.

This works for instance for `/nxt/getState` or `/nxt/getBalance` and will return a formatted HTML5 page with results. Account ID is set automatically from the currently logged in user.


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

![FXC call graph](freecoin_fxc.png)

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

![SecretShare call graph](freecoin_secretshare.png)

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
