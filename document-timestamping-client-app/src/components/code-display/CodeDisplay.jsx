import { useState } from "react";
import copyToClipboard from "../../util/clipboard";

const CodeDisplay = (props) => {
  const [isCopyBtnClicked, setIsClickedCpyBtn] =
    useState(false);
  const [isCodeShown, setIsCodeShown] = useState(false);

  const onCopyButtonClicked = async (event) => {
    event.preventDefault();
    setIsClickedCpyBtn(await copyToClipboard(props.code));
  };

  const onShowCodeButtonClicked = (event) => {
    event.preventDefault();
    setIsCodeShown(!isCodeShown);
  };

  return (
    <div>
      <div className="row mt-2 mb-2">
        <div className="col">
        <button
            className="btn btn-dark me-2"
            onClick={onShowCodeButtonClicked}
          >
            Show / Hide code
          </button>

          <button
            className="btn btn-dark"
            onClick={onCopyButtonClicked}
          >
            Copy code
          </button>
          
        </div>
      </div>

      {isCopyBtnClicked && (
        <div className="row mt-2">
          <div className="col">
            <h6 className="">Code copied to clipboard!</h6>
          </div>
        </div>
      )}
      {isCodeShown && <pre>{props.code}</pre>}
    </div>
  );
};

export default CodeDisplay;
