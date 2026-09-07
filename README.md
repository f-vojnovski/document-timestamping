# Document Timestamping

A trusted timestamping service. Upload a document and get back a cryptographic proof that
the file existed in exactly that form at a particular moment. The proof is a signed hash,
and checking it requires no contact with the server. The document, a public key and a short
Java program are enough.

Built by Filip Vojnovski (171214) and Jovan Canevski (185058) as coursework for
Information Security. The original Macedonian write-up is preserved at
[README.mk.md](README.mk.md).

## The idea

A hash proves a document has not changed, but says nothing about when it existed. Adding a
timestamp from someone willing to sign for it turns the hash into evidence of both.

That evidence is only as good as the signature behind it. Anyone can claim a document is
from last March; only the holder of the private key can produce a signature that the
matching public key will open. The server signs, the client keeps the signature, and the
proof survives the server going offline for good, provided the private key was never
compromised.

## Modules

| Module | Stack | Role |
| --- | --- | --- |
| [document-timestamping-api](document-timestamping-api/) | Spring Boot 2.6.4, Java 11, JPA/Hibernate, PostgreSQL | Hashes, timestamps and signs documents |
| [document-timestamping-client-app](document-timestamping-client-app/) | React 17, Bootstrap 5, axios | Upload and verification UI, generates the verifier source |
| [document-timestamping-client-verification](document-timestamping-client-verification/) | Plain Java, JDK only | Standalone offline verifier |

## How the server timestamps a document

The API exposes one main endpoint, `POST /api/v1/documents/`, taking a title and a file. A
valid request goes to the document service, which runs the hashing and signing pipeline.

### 1. Hash the document

`FileChecksumCalculator.getFileChecksum(MessageDigest, MultipartFile)` streams the upload
through the digest in 1024-byte chunks and returns the checksum. SHA-512 is used for its
collision resistance: producing two documents with the same hash is not practically
achievable, so the checksum can stand in for the document itself.

### 2. Read the server clock

`TimestampingUtility.getCurrentTime()` returns a `java.sql.Timestamp` built from
`System.currentTimeMillis()`. The reading comes from the server rather than the client,
since the client is the one with a reason to misreport it.

### 3. Bind the document to the time

`FileTimestamp.hashFileWithTimestamp` writes the 64-byte checksum into a byte stream, appends
the timestamp, and runs SHA-512 over the combined buffer. The result is a single hash covering
both the document and the moment it arrived. In the current implementation the appended value
is `Long.byteValue()` of the epoch milliseconds, so one byte of the clock reading enters the
digest; the full millisecond value is stored and returned to the client alongside it.

### 4. Sign the combined hash

`CipherUtility.signDocumentHash` runs the combined hash through an RSA cipher in encrypt mode
using the private key from the keystore. A 2048-bit key turns the 64-byte hash into a 256-byte
signature. Knowing the algorithm and holding the public key is not enough to repeat this step;
that requires the private key.

### 5. Persist and respond

The `Document` entity records the title, the signature, the document checksum, the combined
hash and the timestamp. The public key is marked `@Transient`: it travels to the client in the
response but is not a database column, because it is derived from the certificate rather than
owned by the row.

The client receives the signed hash, the timestamp, the public key from the certificate and
the hashing algorithm, which together cover everything needed to check the proof without the
server.

### Response

```json
{
  "id": 1,
  "title": "Ducks research paper",
  "encryptedHash": "4635E7D8B530DB59511316244E2AAC4D...",
  "documentChecksum": "EAF7542ADE2C338D8D2CC76FCBF883E6...",
  "targetHash": "3A20068924764C1AE4168785EE92DBE9...",
  "timestamp": 1788823767103,
  "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAojL8zi8c..."
}
```

| Field | Meaning |
| --- | --- |
| `documentChecksum` | SHA-512 of the file on its own |
| `timestamp` | Epoch milliseconds read from the server clock |
| `targetHash` | SHA-512 of the checksum combined with the timestamp |
| `encryptedHash` | `targetHash` signed with the RSA private key |
| `publicKey` | Base64 X.509 public key, read from the certificate |

This response is the proof, so save it somewhere safe.

## Verifying without the server

[document-timestamping-client-verification](document-timestamping-client-verification/) is a
plain Java project with no build file, no dependencies and no network code. It holds two
classes that run on the user's own machine and let a proof be checked when the server is
unavailable.

`ChecksumGenerator` rebuilds `targetHash` from scratch: it reads the document off disk, hashes
it with SHA-512, applies the timestamp the same way the server did, and prints the result.
That gives the combined hash this file should produce at the claimed time.

`Main` opens the signature. It decodes the Base64 public key through `X509EncodedKeySpec` and
`KeyFactory`, decrypts `encryptedHash` with an RSA cipher in decrypt mode, and compares the
recovered bytes against `targetHash`. It prints `KEYS MATCH - OK!` or `DOCUMENT NOT VALID!`.

Run both and compare against the response. A matching `targetHash` from `ChecksumGenerator`
means the document on disk is the one that was uploaded. The same `targetHash` recovered by
`Main` from the signature means it was signed by the private key behind the certificate.
Neither check contacts the server.

The web client writes both files with the correct values already filled in, so verification
comes down to compiling and running them. `sample.pdf` ships with the module as a document to
try it against.

```
$ javac com/ib/*.java

$ java com.ib.ChecksumGenerator
3A20068924764C1AE4168785EE92DBE9261E43DC2160A4DE68C4326B8412386989B03F77AEDEDB63F7163B173AC1F1F3D765F15E196EA5771CE34CB36538DD8D

$ java com.ib.Main
DECRYPTED HASH:
3A20068924764C1AE4168785EE92DBE9261E43DC2160A4DE68C4326B8412386989B03F77AEDEDB63F7163B173AC1F1F3D765F15E196EA5771CE34CB36538DD8D
KEYS MATCH - OK!
```

Change a single byte of the document and `ChecksumGenerator` prints a completely different
hash, which no longer matches what the signature opens to.

## Verifying against the API

`POST /api/v1/documents/verify` covers the case where the user still has the document but has
lost the proof. It hashes the uploaded file and looks the checksum up through
`DocumentRepository.findFirstByDocumentChecksum`, returning the stored record with the public
key attached. This route needs the server, unlike the offline one above.

## Keys and certificates

The server signs with an RSA key pair held in a PKCS#12 keystore and hands out the public half
from an X.509 certificate. Splitting them into two stores keeps the roles clear: the keystore
holds the private key and stays on the server, the truststore holds only the certificate and
is safe to distribute.

The certificate does more than carry a public key. If a hash decrypts correctly under a public
key issued by a Certificate Authority and matches the expected hash, the message is
established as signed by the holder of that certificate. The certificate generated below is
self-signed, which is fine for running the project; moving to a CA-issued certificate or a
full chain needs a different keystore, not different code.

`SecureKeysManager` loads both stores. Every location, alias, type and password is read from
configuration, so a deployment points at its own keystore without a rebuild. Locations accept
any Spring resource prefix: `classpath:` while developing, `file:` for a keystore mounted
outside the jar. The public key sent to clients is read from the certificate at request time,
so generating a new keystore is all it takes for the generated verifier code to work against
it.

`BytesHexConverter` handles the hex encoding used for every hash that crosses into the
database or out to the client.

### Generating your own keystore

No key material is committed to this repository. Generate your own with `keytool`, which ships
with the JDK. Run these from `document-timestamping-api/src/main/resources/cipher/`, or
anywhere you prefer if you point `KEYSTORE_LOCATION` at it.

Create the signing key pair and its self-signed certificate:

```bash
keytool -genkeypair \
  -alias senderKeyPair \
  -keyalg RSA -keysize 2048 \
  -dname "CN=TimestampingSite" \
  -validity 365 \
  -storetype PKCS12 \
  -keystore sender_keystore.p12 \
  -storepass "$KEYSTORE_PASSWORD"
```

Export the certificate:

```bash
keytool -exportcert \
  -alias senderKeyPair \
  -keystore sender_keystore.p12 \
  -storepass "$KEYSTORE_PASSWORD" \
  -file sender_certificate.cer
```

Import it into the store the server reads public keys from:

```bash
keytool -importcert \
  -alias receiverKeyPair \
  -keystore receiver_keystore.p12 \
  -storetype PKCS12 \
  -storepass "$KEYSTORE_PASSWORD" \
  -file sender_certificate.cer \
  -noprompt
```

For PKCS#12 the key password and the store password must match, so leave `-keypass` off and
let it default. Confirm the result with
`keytool -list -keystore sender_keystore.p12 -storepass "$KEYSTORE_PASSWORD"`, which should
show `senderkeypair` as a `PrivateKeyEntry`, and the same command against
`receiver_keystore.p12`, which should show `receiverkeypair` as a `trustedCertEntry`.

Keystores contain private keys and must not be committed. The `cipher/` directory is
gitignored.

## Running it

Requirements: JDK 11 or newer, PostgreSQL, Node 16 or newer.

Create the database:

```bash
createdb documenttimestamping
```

Set the environment. Neither password has a default, so the application will not start
without them:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/documenttimestamping"
export DB_USERNAME="postgres"
export DB_PASSWORD="your-database-password"
export KEYSTORE_PASSWORD="your-keystore-password"
```

Start the API on port 8080:

```bash
cd document-timestamping-api
./mvnw spring-boot:run
```

Start the web client on port 3000:

```bash
cd document-timestamping-client-app
npm install
npm start
```

### Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/documenttimestamping` | JDBC connection string |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | none, required | Database password |
| `KEYSTORE_LOCATION` | `classpath:cipher/sender_keystore.p12` | Keystore holding the private key |
| `KEYSTORE_PASSWORD` | none, required | Keystore password |
| `KEYSTORE_TYPE` | `PKCS12` | Keystore format |
| `KEYSTORE_KEY_ALIAS` | `senderKeyPair` | Alias of the private key entry |
| `TRUSTSTORE_LOCATION` | `classpath:cipher/receiver_keystore.p12` | Store holding the certificate |
| `TRUSTSTORE_PASSWORD` | falls back to `KEYSTORE_PASSWORD` | Truststore password |
| `TRUSTSTORE_TYPE` | `PKCS12` | Truststore format |
| `TRUSTSTORE_CERTIFICATE_ALIAS` | `receiverKeyPair` | Alias of the certificate entry |

Uploads are capped at 5 MB per file and 6 MB per request.

## API

| Method | Path | Body | Returns |
| --- | --- | --- | --- |
| `POST` | `/api/v1/documents/` | `title`, `file` | The document record with its proof |
| `POST` | `/api/v1/documents/verify` | `file` | The stored record if the checksum is known |

## Web client

The React app handles uploading and verifying. Pick a file, give it a title, and press Upload
to send it to the API.

The response is rendered as readable fields with a button to copy the whole payload. Below
that, the app generates the two Java classes described above with the signature, timestamp,
public key and target hash already filled in, each behind show/hide and copy controls.

## Layout

```
document-timestamping-api/
  src/main/java/com/documenttimestamp/
    api/           DocumentController
    service/       DocumentService
    repository/    DocumentRepository
    model/         Document
    timestamping/  FileChecksumCalculator, TimestampingUtility, FileTimestamp,
                   CipherUtility, SecureKeysManager, BytesHexConverter
document-timestamping-client-app/
  src/components/  DocumentUpload, DocumentHashData, CodeDisplay
  src/util/        DriverCodeGenerator, ChecksumCodeGenerator
document-timestamping-client-verification/
  src/com/ib/      ChecksumGenerator, Main
  sample.pdf
```

## References

- [Trusted timestamping](https://en.wikipedia.org/wiki/Trusted_timestamping)
- [What is a Timestamping Authority](https://blog.ascertia.com/what-is-a-timestamping-authority)
- [Digital signatures](https://www.techtarget.com/searchsecurity/definition/digital-signature)
- [Java digital signatures, Baeldung](https://www.baeldung.com/java-digital-signature)
- [SHA-512 hash in Java](https://www.geeksforgeeks.org/sha-512-hash-in-java/)
- [Connect to PostgreSQL from Spring Boot](https://www.codejava.net/frameworks/spring-boot/connect-to-postgresql-database-examples)
- [Spring Boot file upload](https://mkyong.com/spring-boot/spring-boot-file-upload-example/)
- [File upload in React](https://www.laravelcode.com/post/how-to-upload-files-in-reactjs-with-example)

## License

MIT, see [LICENSE](LICENSE).
