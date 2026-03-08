# Research Report

## Why
As we are holding cyrpto accounts as custodial accounts its important that we have a way to store these custodial account private keys safely. As such
I am investing Java AES encryption to keep these keys safe in our database with a predictable way to encyrpt and unencrypt them as needed.

## Java AES Encryption and Decryption Implementation
I investigated key block ciphers. Specifically with AES, using java's cryptography architecture. I implemented a really simple command-line java tool
to encrypt and decrypt basic text files using AES-GCM. I used PBKDF2 for password-based key creation for consistent keys. The project is structured to
run via a simple Makefile that loads environment vars from the .env.
Time Spent

## Time Spent

~15 minutes: Reviewed the article
~30 minutes: Created and tried to document the AESEncryption class for others to view
~20 minutes: Built the CLI frontend so people toying with this research could have a hands on example
~10 minutes: Figuring out how to create and configure the makefile to setup the environment
~10 minutes: Writing this report

## Results
I produced a simple Java CLI tool for encrypting and dencrypting basic text files. Key takeaways from this implementation are as follows:
- Encryption Mode: AES supports a lot of different modes, but according to baeldung AES/GCM is the recommended modern aproach. GCM provides some build in authentication including the extra cradentials from the key. This protects the key from tampering.[^1]
- Instead of generating random keys. I made the assumption we will use a strong secret key that can be reliably derived from a human-readable environment passphrase and salt using the PBKDF2WithHmacSHA256 key factory. Long name, but it uses Sha256 to help derive a passphrase in short.
- Initialization Vectors. GCM mode requires some uniqueness, 12-byte pseudo-random IV for every encryption operation to remain secure. Because this exact IV is needed during decryption the process reqiures the same IV, as such we must store it alongside the ciphertext in some way. I solved this by prepending my Base64-encoded IV to the top of the encrypted file output. Though I think this is likely a frowned upon approach.
- Build with makefile. I setup a makefile run the file to make managing the environment variables easier for those who wish to use this system.

### Sources

- Baeldung Java AES Encryption Decryption[^1]

[^1]: https://www.baeldung.com/java-aes-encryption-decryption
