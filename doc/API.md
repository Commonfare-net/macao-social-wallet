---
title: Freecoin API
---

# Introduction

Welcome developers! This API is currently under re-design. The public repository for this software is on [github.com/d-cent/freecoin](https://github.com/d-cent/freecoin)

Freecoin uses device based entitlement.

The API KEY is obtained on wallet creation or restore, we may refer to it simply as "key". All other fields are highlighted `this way`.

All GET operations presume to have the key in place as session cookie.

All failing operations return an HTTP error code and a json structure including the key `problems` followed by an explanation of the error that has occurred.

As this is a work in progress, each section reports API completion with TODO and DONE sections.

# Balance

The balance endpoint is the default and will redirect to the creation of a new wallet if the client is not associated with one yet. It will visualize a card and the information on the current balance and a recent history of transactions.

## GET /

Return the balance as a static html page.

# Participants

The participants endpoint provides access to the public facing information for every known participant, returning `cards` with name, email and a qrcode that can be used to send any amount of value to the participant, linking directly to the `send` endpoint.

## GET /participants

Return a search form where partcipants can be looked up by name or email


## GET /participants/all

Shows a list of cards of all known participants, in plain static html, including name, email and qrcode.

## GET /participants/find?

Accepts two url encoded parameters, one is the `field` to be searched (`name` or `email` and other wallet fields) and the other is the `value` to search for.

Returns the card of the found participant or an html page containing "No participant found".

#### example request
GET http://localhost:8000/participants/find?field=email&value=jaromil@dyne.org

## GET /participants/name

Return the card of the `name`d participant.

## GET /participants/name/qrcode

Retrieve the QRCode image for the participant corresponding to `name`. The QR image can be scanned to redirect to the `/send` url to transfer funds to the participant. The returned value is an actual `image/png` content-type. This url can be directly used inside `<img src="">` fields.

## GET /participants/name/avatar

Retrieve the avatar image for the participant corresponding to `name`. The avatar is retrieved from the GRavatar on-line service (wordpress) and is returned with content-type `image/png`. This url can be directly used inside `<img src="">` fields.


# Wallet

A `wallet` is basically a private account. This endpoint provides services to create, recover or destroy a wallet.

## GET /wallets

Will respond with an html form to create a wallet, following up with configured confirmations.

If the caller has already created a wallet, will act as the `/` endpoint, returning the current balance.

## POST /wallets

Endpoint for the GET form, accepts fields `name` and `email`.

Check for duplicate name and email in database, if available returns success with a `confirmation` hash.

If content-type is `text/html`, the browser is redirected to `GET /wallets/hash` (described below)

Graft here any other mechanism for sophisticated confirmation, also bound to external applications or email confirmation.


<!-- #### example response -->

<!--     { -->
<!--         "name": "nickname" -->
<!--         "email": "email@address" -->
<!--         "balance": float -->
<!--         "created": date of wallet creation -->
<!--         "last_access": date of last access -->
<!--         "last_location": location (IP) of last access -->
<!--         "last_transaction": link to the last transaction occurred -->
<!--     } -->

## GET /wallets/hash

Returns a simple page with a confirmation button.

<!-- TODO: Check here if the `hash` is valid? right now is checked in the next one. -->
<!-- Leaving this here may help for tracing malicious attempts. -->

## POST /wallets/hash

Checks if the `hash` is valid (present in the confirmations collection) and actually creates the wallet as requested.

<!-- TODO: Perhaps create a generic confirmation endpoint for a linked-list (closure) of actions to be executed. -->


## POST /wallets/recover

TODO

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

# Vouchers

A voucher is a pre-defined amount that can be transferred off-line using a secret code or image (qrcode)

Using vouchers is possible to make intuitive a POS transaction:

1. participant scans the price to be paid, link to /voucher/create url
2. wallet respond with a QRcode that is a /voucher/claim/:voucher-id url
3. participant offers the QRcode to the vendor for scanning
4. vendor scans the /voucher/claim url and confirms payment

- TODO
  - API design
  - GET /voucher/create/:amount
  - POST /voucher/create
  - GET /voucher/claim/:voucher-id
  - POST /voucher/claim

## GET /voucher/create/:amount

Create a signed voucher of `amount` ready to be given, returns human readable stash-id and QR image.

## POST /voucher/create

Same as GET, but with POST field `amount`

## GET /voucher/claim/:stash-id

Claim a voucher by url (can also be a direct link from qrcode)

## POST /voucher/claim

Same as GET, but with POST field `stash-id`



<!-- # Vendor relations -->

<!-- Vendor value transactions must be customized ad-hoc for pilots: transport companies, tax departments, service providers. -->

<!-- - TODO -->
<!--   - API design -->
<!--   - GET /vendor/send/:amount/:vendor -->
<!--   - GET /vendor/send/:amount/:vendor/:participant -->
<!--   - POST /vendor/send -->
<!--   - vendor plugins -->


<!-- ## GET /vendor/send/:amount/:vendor -->

<!-- Send `amount` to `vendor` in exchange for service credit for self, returns human readable message, all accepted fields and an id linking to the `transaction`. -->

<!-- ## GET /vendor/send/:amount/:vendor/:participant -->

<!-- Send `amount` to `vendor` in exchange of service credit for another `participant`, returns human readable message, all accepted fields plus an id linking to the `transaction`. -->

<!-- ## POST /vendor/send -->

<!-- Same as GET operations, with POST fields `amount` and `vendor`, optional field `participant`. -->


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


    P = Participant (passcard keys)
    O = Organization (online portal)
    B = Auditor (secondary online portal and/or cold storage)

         :HI        :LO
    FXC1_random_FXC_random_1 - P     } participant's web cookie

    FXC1_random_FXC_random_2 - P     } participant's passcard
    FXC1_random_FXC_random_3 - P     } participant's passcard

    FXC1_random_FXC_random_4 - O & P } participant's passcard
    FXC1_random_FXC_random_5 - O     } organization
    FXC1_random_FXC_random_6 - O     } organization

    FXC1_random_FXC_random_7 - O & B } auditor
    FXC1_random_FXC_random_8 - B     } auditor
    FXC1_random_FXC_random_9 - B     } auditor

This distribution allows the organization to serve access to blockchain operations accepting a single FXC slice stored in the participant's web browser cookie that was set upon authentication.  The participant has also a QRCode and/or SIMcard and/or USB key where other slices of the key are stored and can be combined with those of the auditor for blockchain access. Of course also the organization can grant access to participants via the use of a physical key.


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




<!-- # More TODO -->

<!-- Progress using http://swagger.io -->

<!-- Ring-swagger https://github.com/metosin/ring-swagger -->

<!-- Defining schema using Prismatic https://github.com/Prismatic/schema -->
