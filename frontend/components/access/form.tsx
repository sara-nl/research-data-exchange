import {
  Button,
  Col,
  Form as RForm,
  FormGroup as RFormGroup,
  Alert,
} from 'react-bootstrap';
import { Formik, Field, FieldProps } from 'formik';
import { Dataset } from '../../types';
import React, { useState } from 'react';
import dynamic from 'next/dynamic';
import FieldFeedback from '../form/fieldFeedback';
import { Tooltip } from 'react-bootstrap';
import { OverlayTrigger } from 'react-bootstrap';
import AgreeToConditions from './form/AgreeToConditions';

const PDFViewer = dynamic(() => import('../pdf-view'), {
  ssr: false,
});

export type Values = {
  agree: boolean;
  email: string;
  name: string;
};

type Props = {
  dataset?: Dataset;
  storeValues: (values: Values) => Promise<Values>;
  onSuccessSubmission: (values: Values) => Promise<void> | void;
};

const AccessForm: React.FC<Props> = ({
  dataset,
  storeValues,
  onSuccessSubmission,
}) => {
  const [scrollBottom, setScrollBottom] = useState(0);
  const [downloadedConditions, setDownloadedConditions] = useState(false);

  const handleScroll = (e) => {
    const { target } = e;
    if (target.scrollHeight - target.scrollTop === target.clientHeight) {
      let i = scrollBottom;
      i += 1;
      setScrollBottom(i);
    }
  };

  const [outOfService, setOutOfService] = useState<string | undefined>(
    undefined,
  );

  const canAgree = scrollBottom >= 1 || downloadedConditions;

  if (outOfService) {
    return (
      <Alert variant="danger" className="mb-5">
        <p className="mb-0">
          We could not request acess for dataset because of error :
          {outOfService}. Please email rdx@surf.nl describing this problem.
        </p>
      </Alert>
    );
  }

  return (
    <Formik
      initialValues={{ agree: false, name: '', email: '' }}
      validate={(values) => {
        const errors: {} = {};

        if (!values.email) {
          errors['email'] = 'Please enter valid email address';
        } else if (
          !/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$/i.test(values.email)
        ) {
          errors['email'] = 'Please enter valid email address';
        }

        if (!values.name) {
          errors['name'] = 'Please enter the name.';
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
            <Alert variant="info" className="px-5">
              <h4>You are about to request access to a dataset</h4>
              Please read the following use conditions carefully. You can
              download the dataset only if you fully agree to them. Your name
              and email address along with the fact of agreement will be stored
              in our system in order to ensure that the data is used
              appropriately.
            </Alert>
          </div>
          <div className="pdf-view" onScroll={handleScroll}>
            <PDFViewer conditionsUrl={dataset.conditionsUrlProxy} />
          </div>

          <RForm noValidate validated={!errors} onSubmit={handleSubmit}>
            <p className="text-center">
              <a
                className="badge badge-secondary"
                download="Research_Data_Exchange_Conditions"
                href={dataset.conditionsUrl}
                onClick={() => setDownloadedConditions(true)}
              >
                Download PDF
              </a>
            </p>

            <RForm.Row className="mt-2">
              <Field name="agree">
                {(fp: FieldProps) => (
                  <RForm.Group
                    as={Col}
                    controlId="agree"
                    className="text-center"
                  >
                    {!canAgree ? (
                      <OverlayTrigger
                        placement="bottom-start"
                        overlay={
                          <Tooltip id="tooltip-disabled">
                            To agree you must fully read or download the
                            conditions first
                          </Tooltip>
                        }
                      >
                        <AgreeToConditions
                          disabled={!canAgree}
                          formikFieldProps={fp}
                        />
                      </OverlayTrigger>
                    ) : (
                      <AgreeToConditions
                        disabled={!canAgree}
                        formikFieldProps={fp}
                      />
                    )}
                  </RForm.Group>
                )}
              </Field>
            </RForm.Row>
            <RForm.Row>
              <Field name="name">
                {(fp) => (
                  <RForm.Group as={Col} controlId="name">
                    <RForm.Label>
                      <span className="lead">Name</span>{' '}
                      <sup className="font-weight-light">required</sup>
                    </RForm.Label>
                    <RForm.Control
                      required
                      type="text"
                      className="name-text"
                      disabled={!values.agree}
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
            </RForm.Row>
            <RForm.Row>
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
                      disabled={!values.agree}
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
            </RForm.Row>

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
                <span>Request Access</span>
              )}
            </Button>
          </RForm>
        </React.Fragment>
      )}
    </Formik>
  );
};

export default AccessForm;
