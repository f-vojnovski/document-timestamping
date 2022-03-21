import { useState } from "react";

const DocumentHashData = (props) => {
  const [isCopyBtnClicked, setIsClickedCpyBtn] =
    useState(false);

  const onCopyButtonClicked = (event) => {
    event.preventDefault();
    navigator.clipboard.writeText(props.copyData);
    setIsClickedCpyBtn(true);
  };

  return (
    <div>
      <div className="row">
        <div className="col">
          <span className="text-primary">ID: </span>
          <span>{props.id}</span>
        </div>
      </div>
      <div className="row">
        <div className="col">
          <span className="text-primary">Title: </span>
          <span>{props.title}</span>
        </div>
      </div>
      <div className="row">
        <div className="col">
          <span className="text-primary">
            Encrypted hash:{" "}
          </span>
          <span>{props.encryptedHash}</span>
        </div>
      </div>
      <div className="row">
        <div className="col">
          <span className="text-primary">Document hash: </span>
          <span>{props.checksum}</span>
        </div>
      </div>
      <div className="row">
        <div className="col">
          <span className="text-primary">Document + timestamp hash: </span>
          <span>{props.checksumWithTimestamp}</span>
        </div>
      </div>
      <div className="row">
        <div className="col">
          <span className="text-primary">Timestamp: </span>
          <span>{props.timestamp}</span>
        </div>
      </div>
      <div className="row">
        <div className="col">
          <span className="text-primary">
            Public key data:{" "}
          </span>
          <span>{props.pk}</span>
        </div>
      </div>
      <div className="row mt-2">
        <div className="col">
          <button
            className="btn btn-dark"
            onClick={onCopyButtonClicked}
          >
            <i className="fa fa-folder"></i>Copy response
          </button>
        </div>
      </div>

      {isCopyBtnClicked && (
        <div className="row mt-2">
          <div className="col">
            <h6 className="">
              Response copied to clipboard!
            </h6>
          </div>
        </div>
      )}
    </div>
  );
};

export default DocumentHashData;
