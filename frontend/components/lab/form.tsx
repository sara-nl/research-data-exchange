import {
  Button,
  Col,
  Form as RForm,
  Row,
  FormGroup as RFormGroup,
  Alert,
} from 'react-bootstrap';
import { Formik, Field, FieldProps } from 'formik';
import { Dataset } from '../../types';
import React, { useState } from 'react';
import FieldFeedback from '../form/fieldFeedback';


export type Values = {
  script_location: string;
};

type Props = {
  storeValues: (values: Values) => Promise<Values>;
  onSuccessSubmission: (values: Values) => Promise<void> | void;
};

const JobForm: React.FC<Props> = ({
  storeValues,
  onSuccessSubmission,
}) => {

  const [outOfService, setOutOfService] = useState<string | undefined>(
    undefined,
  );

  if (outOfService) {
    return (
      <Alert variant="danger" className="mb-5">
        <p className="mb-0">
          We could not submit your analysis job because of an error :
          {outOfService}. Please email rdx@surf.nl describing this problem.
        </p>
      </Alert>
    );
  }

  return (
    <Formik
      initialValues={{ script_location: '' }}
      validate={(values) => {
        const errors: {} = {};

        if (!values.script_location) {
          errors['script_location'] = 'Please enter a script location';
        } else if (
          !/((https?):\/\/)?(www.)?[a-z0-9]+(\.[a-z]{2,}){1,3}(#?\/?[a-zA-Z0-9#]+)*\/?(\?[a-zA-Z0-9-_]+=[a-zA-Z0-9-%]+&?)?$/i.test(values.script_location)
        ) {
          errors['script_location'] = 'Please enter a valid url';
        }

        return errors;
      }}
      onSubmit={async (values, actions) => {
        await storeValues(values)
          .catch((e) => {
            setOutOfService(e);
          })
          .then(onSuccessSubmission);
        actions.setSubmitting(false);
      }}
    >
      {({
        handleChange,
        handleSubmit,
        handleBlur,
        values,
        errors,
        isValidating,
        isSubmitting,
        isValid,
        dirty,
      }) => (
        <React.Fragment>
          <div>
            <Alert key="info" variant="info">
              <h4>You are about to run an analysis on a dataset</h4>
              The analysis environment expects a Python script that can be downloaded via a publically accessible git repository, like GitHub or GitLab.
              The main directory of this repository can have the following files:
              <ul>
                <li>script.py (required)</li>
                <li>requirements.txt (optional) - for installing package dependencies</li>
                <li>other files used in script.py (optional)</li>
              </ul>
              Your script will be run with the following arguments:
              <pre>script.py -i $1 -o $2 -t $3</pre>
              You should use them in the following way:
              <ul>
                <li>-i is the input folder that contains the dataset files you see on the right</li>
                <li>-o is the output folder to which you can write the results of your analysis that should be saved</li>
                <li>-t is the temp folder to which you can write intermediate results that should <b>not</b> be saved</li>
              </ul>
              Note: error messages, logs and print statements will also be saved.
              You can find an example of an algorithm <a href="https://gitlab.com/surftim/rdx-tools-to-data/-/blob/main/ansible/plugin-tools-to-data/files/example_script.py">here</a>.
            </Alert>
          </div>


          <RForm noValidate validated={!errors} onSubmit={handleSubmit}>
            <Row>
              <Field name="script_location">
                {(fp) => (
                  <RForm.Group as={Col} controlId="script_location">
                    <RForm.Label>
                      <span className="lead">Script location</span>{' '}
                      <sup className="font-weight-light">required</sup>
                    </RForm.Label>
                    <RForm.Control
                      required
                      type="url"
                      className="url-text"
                      value={fp.field.value}
                      onBlur={fp.field.onBlur}
                      onChange={fp.field.onChange}
                      isValid={fp.meta.touched && !fp.meta.error}
                      isInvalid={fp.meta.touched && fp.meta.error}
                    />
                    <FieldFeedback {...fp} />
                    <span>A public GitHub or GitLab repo with a script.py file in its main directory.</span>
                  </RForm.Group>
                )}
              </Field>
            </Row>

            <Button
              disabled={!dirty || !isValid || isSubmitting}
              type="submit"
              variant="primary"
            >
              {isSubmitting ? (
                <span
                  className="spinner-border spinner-border-sm mr-5 ml-5"
                  role="status"
                  aria-hidden="true"
                />
              ) : (
                <span>Submit Analysis Job</span>
              )}
            </Button>
          </RForm>
        </React.Fragment>
      )}
    </Formik>
  );
};

export default JobForm;
