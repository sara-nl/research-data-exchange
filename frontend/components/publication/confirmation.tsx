import {
  Container,
  Row,
  Button,
  Col,
  Form as RForm,
  InputGroup,
  Alert,
} from 'react-bootstrap';
import { Formik, Field } from 'formik';

type Props = { accessUrl: string };

const PublicationConfirmation: React.FC<Props> = ({ accessUrl }) => {
  return (
    <Alert variant="success" className="px-5">
      <h4 className="alert-heading">Well done!</h4>
      <p>✨ Your dataset has been published under use conditions. What now?</p>

      <p className="text-center py-3 text-nowrap overflow-auto border border-info">
        <samp>{accessUrl}</samp>
      </p>

      <h5 className="text-center">1. Your dataset is published</h5>

      <p className="text-justify">
        This link points to the public page containing the use conditions of the
        dataset and guides the researcher through obtaining access to the
        dataset. The researcher will only receive the dataset after RDX ensures
        that the request meets all necessary conditions.
      </p>

      <h5 className="text-center">2. Spread the word</h5>
      <p className="text-justify">
        Anyone can request access to the dataset, but researchers may need help
        finding the page they need to use. Please share the link with the
        relevant audience yourself (e.g. by adding it to the dataset page in the
        catalogue or repository).
      </p>
      <h5 className="text-center">3. Get notified</h5>
      <p className="text-justify">
        You'll get an email when someone requests access to this dataset.
      </p>
      <h5 className="text-center">4. Get insights</h5>
      <p className="text-justify">
        You can see an overview of the datasets shared with RDX in the <a href="/dashboard">RDX Dashboard</a>.
      </p>
      <hr />
      <p className="text-center alert-link">
        <a href={accessUrl}>Go to the dataset access page</a>.
      </p>
    </Alert>
  );
};

export default PublicationConfirmation;
