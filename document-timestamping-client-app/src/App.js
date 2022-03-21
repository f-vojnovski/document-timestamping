import "./App.css";
import "bootstrap/dist/css/bootstrap.min.css";
import * as consts from "./constants.jsx";
import { useEffect, useState } from "react";
import DocumentUpload from "./components/pages/document-uplooad/DocumentUpload";

const App = () => {
  return <DocumentUpload></DocumentUpload>;
};

export default App;
