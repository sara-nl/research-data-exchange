import {
  Button,
  Col,
  Form as RForm,
  Row,
  FormGroup as RFormGroup,
  Alert,
} from 'react-bootstrap';
import { Formik, Field, FieldProps } from 'formik';
import React, { useState } from 'react';
import dynamic from 'next/dynamic';
import FieldFeedback from '../../form/fieldFeedback';


const PDFViewer = dynamic(() => import('../../pdf-view'), {
  ssr: false,
});

export type Values = {
  email: string;
  role: string;
};

type Props = {
  storeValues: (values: Values) => Promise<Values>;
  onSuccessSubmission: (values: Values) => Promise<void> | void;
};

const SignInForm: React.FC<Props> = ({
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
          We could not sign you in to the RDX Dashboard because of the following error :
          {outOfService}. Please email rdx@surf.nl describing this problem.
        </p>
      </Alert>
    );
  }

  return (
    <Formik
      initialValues={{ email: '', role: '' }}
      validate={(values) => {
        const errors: {} = {};

        if (!values.role) {
          errors['role'] = 'Please select a role';
        }

        if (!values.email) {
          errors['email'] = 'Please enter valid email address';
        } else if (
          !/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$/i.test(values.email)
        ) {
          errors['email'] = 'Please enter valid email address';
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
          <Row>
            <Col></Col>
            <Col><h1 className='mb-3'>📊 RDX Dashboard</h1></Col>
          </Row>
          <RForm noValidate validated={!errors} onSubmit={handleSubmit}>
          <Row>
              <Col></Col>
              <Field name="role">
                {(fp) => (
                  <RForm.Group as={Col} controlId="role">
                    <RForm.Label>
                      <span className="lead">Sign in as:</span>{' '}
                    </RForm.Label>
                    <RForm.Check
                      type="radio"
                      name="role"
                      label="Researcher"
                      value="researcher"
                      onChange={e => {
                        // Note: Since radio buttons remain focused after selection,
                        // we need to manually blur them to immediately update .touched
                        // (including the first click)
                        e.currentTarget.blur();
                        handleChange(e);
                      }}
                    />
                    <RForm.Check
                      type="radio"
                      name="role"
                      label="Data Steward"
                      value="data_steward"
                      onChange={e => {
                        // Note: Since radio buttons remain focused after selection,
                        // we need to manually blur them to immediately update .touched
                        // (including the first click)
                        e.currentTarget.blur();
                        handleChange(e);
                      }}
                    />
                    <FieldFeedback {...fp} />
                  </RForm.Group>
                )}
              </Field>

            </Row>
            <Row>
              <Col></Col>
              <Field name="email">
                {(fp) => (
                  <RForm.Group as={Col} controlId="email">
                    <RForm.Label>
                      <span className="lead">Email</span>{' '}
                      <sup className="font-weight-light">required</sup>
                    </RForm.Label>
                    <RForm.Control
                      required
                      type="email"
                      className="email-text"
                      value={fp.field.value}
                      onBlur={fp.field.onBlur}
                      onChange={fp.field.onChange}
                      isValid={fp.meta.touched && !fp.meta.error}
                      isInvalid={fp.meta.touched && fp.meta.error}
                    />
                    <FieldFeedback {...fp} />
                  </RForm.Group>
                )}
              </Field>

            </Row>
            <Row className="mt-3">
              <Col></Col>
              <Col>
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
                    <span>Sign In</span>
                  )}
                </Button>
              </Col>
            </Row>
          </RForm>
        </React.Fragment>
      )
      }
    </Formik >
  );
};

export default SignInForm;
