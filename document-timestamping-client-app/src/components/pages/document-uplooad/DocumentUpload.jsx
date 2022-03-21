import { useEffect, useState } from "react";
import axios from "axios";
import DocumentHashData from "../../hash-data/DocumentHashData";
import generateDriverCode from "../../../util/DriverCodeGenerator";
import CodeDisplay from "../../code-display/CodeDisplay";
import generateChecksumCode from "../../../util/ChecksumCodeGenerator";

const DocumentUpload = () => {
  const API_URL = "http://localhost:8080/api/";
  const UPLOAD_DOCUMENT_ENDPOINT = `${API_URL}v1/documents/`;
  const VERIFY_DOCUMENT_ENDPOINT = `${API_URL}v1/documents/verify/`;

  const [documentTitle, setDocumentTitle] = useState();
  const [selectedFile, setSelectedFile] = useState();
  const [formError, setFormError] = useState();
  const [response, setResponse] = useState();

  const onDocumentTitleChangedHandler = (event) => {
    event.preventDefault();
    setDocumentTitle(event.target.value);
  };

  const onFileChangedHandler = (event) => {
    var file = event.target.files[0];
    setSelectedFile(file);
  };

  const onFormSubmitted = (event) => {
    if (
      documentTitle == null ||
      selectedFile == null ||
      documentTitle.length == 0
    ) {
      setFormError(
        "Make sure to add a title and upload a file"
      );
      return;
    }
    setFormError(null);

    var data = new FormData();
    data.append("file", selectedFile);
    data.append("title", documentTitle);

    var config = {
      method: "post",
      url: UPLOAD_DOCUMENT_ENDPOINT,
      headers: {},
      data: data,
    };

    axios(config)
      .then(function (response) {
        setResponse(response);
      })
      .catch(function (error) {
        console.log(error);
      });
  };

  const onDocumentVerify = (event) => {
    if (selectedFile == null) {
      setFormError("Upload a file for verification!");
      return;
    }
    setFormError(null);

    var data = new FormData();
    data.append("file", selectedFile);

    var config = {
      method: "post",
      url: VERIFY_DOCUMENT_ENDPOINT,
      headers: {},
      data: data,
    };

    axios(config)
      .then(function (response) {
        setResponse(response);
      })
      .catch(function (error) {
        console.log(error);
      });
  };

  return (
    <div className="App">
      <div className="container-fluid main_container">
        <div className="card mt-2 shadow-lg p-3 mb-2 bg-body rounded">
          <div className="card-body">
            <div className="row">
              <div className="col">
                <h1 className="text-primary">
                  Document Timestamping
                </h1>
              </div>
            </div>

            <div className="row mt-4">
              <div className="col">
                <h4>Upload document</h4>
              </div>
            </div>

            <div className="row mt-2">
              <div className="col">
                <label className="form-label">
                  Document title
                </label>
                <input
                  className="form-control"
                  placeholder="e.g. Ducks research paper"
                  onChange={onDocumentTitleChangedHandler}
                />
              </div>
            </div>

            <div className="row mt-2">
              <div className="col">
                <label className="form-label">
                  Upload your document
                </label>
                <input
                  className="form-control"
                  type="file"
                  onChange={onFileChangedHandler}
                />
              </div>
            </div>

            {formError != null && (
              <div className="row mt-3">
                <div className="col">
                  <div className="alert alert-danger mb-0">
                    {formError}
                  </div>
                </div>
              </div>
            )}

            <div className="row mt-3">
              <div className="col">
                <button
                  type="button"
                  className="btn btn-primary w-100"
                  onClick={onFormSubmitted}
                >
                  Upload
                </button>
              </div>
            </div>

            <div className="row mt-3">
              <div className="col">
                <button
                  type="button"
                  className="btn btn-secondary w-100"
                  onClick={onDocumentVerify}
                >
                  Verify
                </button>
              </div>
            </div>
          </div>
        </div>

        {response && (
          <div>
            <div className="card shadow-lg bg-body rounded ps-4 pe-4 pb-3 mb-2">
              <div className="card-body"></div>
              <h3 className="text-primary">
                Document Hash Data
              </h3>
              <DocumentHashData
                id={response.data.id}
                title={response.data.title}
                encryptedHash={response.data.encryptedHash}
                checksum={response.data.documentChecksum}
                timestamp={response.data.timestamp}
                checksumWithTimestamp={
                  response.data.targetHash
                }
                pk={response.data.publicKey}
                copyData={JSON.stringify(response.data)}
              />
            </div>

            <div className="card shadow-lg bg-body rounded ps-4 pe-4 pb-3">
              <div className="card-body"></div>
              <h3 className="text-primary">Driver code</h3>
              <p>
                This is a driver code written in Java. You
                can use it to prove that your document
                existed in its current state on a given
                date.
              </p>
              <CodeDisplay
                code={generateDriverCode(
                  response.data.encryptedHash,
                  response.data.publicKey,
                  response.data.targetHash
                )}
              />
            </div>

            <div className="card shadow-lg bg-body rounded ps-4 pe-4 pb-3">
              <div className="card-body"></div>
              <h3 className="text-primary">
                Checksum code
              </h3>
              <p>
                You can use this code to generate a checksum
                for your file. In case you happen to{" "}
                <strong>lose</strong> the original file
                checksum generated by the API.
              </p>
              <p>Make sure you replace <strong>sample.pdf</strong> with your file path.</p>
              <CodeDisplay
                code={generateChecksumCode(
                  response.timestamp
                )}
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default DocumentUpload;
