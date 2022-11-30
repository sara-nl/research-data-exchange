import * as React from 'react';
import {
  Row,
  Button,
  Col,
  Form as RForm,
  Alert,
} from 'react-bootstrap';
import { InfoCircle } from 'react-bootstrap-icons';
import { Formik, Field } from 'formik';
import FieldFeedback from './../form/fieldFeedback';
import { useState } from 'react';
import { AccessLicense, Dataset } from '../../types';

type Props = {
  storeValues: (values: Values) => Promise<Dataset>;
  onSuccessSubmission: (dataset: Dataset) => Promise<void> | void;
  header: string;
  dataset: Dataset;
};

export type Values = {
  doi: string;
  title: string;
  authors: string;
  description: string;
  published?: boolean;
};

const PublicationForm: React.FC<Props> = ({
  header,
  dataset,
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
          We could not publish your dataset because of "{outOfService}". Please
          email rdx@surf.nl describing this problem.
        </p>
      </Alert>
    );
  }

  return (
    <React.Fragment>
      <Alert variant="secondary" className="mb-5">
        <p className="mb-0">{header}</p>
      </Alert>

      <Formik
        initialValues={{
          doi: dataset.doi || '',
          title: dataset.title || '',
          authors: dataset.authors || '',
          description: dataset.description || '',
          access_license: dataset.access_license || '',
          published: true
        }}
        validateOnMount={true}
        validateOnChange={true}
        validateOnBlur={false}
        validate={(values) => {
          const errors: {} = {};

          if (!values.doi) {
            errors['doi'] = 'Required';
          } else if (!/^[^\/]+\/.+$/i.test(values.doi)) {
            errors['doi'] =
              'Please enter a valid DOI (e.g. 10.1371/journal.pgen.1001111)';
          }

          if (!values.title) {
            errors['title'] = 'Please enter publication title.';
          }

          if (!values.authors) {
            errors['authors'] = 'Please enter publication authors.';
          }

          if (!values.description) {
            errors['description'] = 'Please enter publication description.';
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
          handleSubmit,
          errors,
          isValid,
        }) => (
          <RForm validated={!errors} onSubmit={handleSubmit}>
            <Row>
              <Field name="doi">
                {(fp) => (
                  <RForm.Group as={Col} controlId="doi">
                    <RForm.Label>
                      <span className="lead">
                        Digital Object Identifier (DOI){' '}
                      </span>
                      <sup className="font-weight-light">required</sup>
                    </RForm.Label>
                    <RForm.Control
                      required
                      type="text"
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
            <Row>
              <Field name="title">
                {(fp) => (
                  <RForm.Group as={Col} controlId="title">
                    <RForm.Label>
                      <span className="lead">Title</span> <sup>required</sup>
                    </RForm.Label>
                    <RForm.Control
                      required
                      type="text"
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
            <Row>
              <Field name="authors">
                {(fp) => (
                  <RForm.Group as={Col} controlId="authors">
                    <RForm.Label>
                      <span className="lead">Authors</span> <sup>required</sup>
                    </RForm.Label>
                    <RForm.Control
                      required
                      type="text"
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
            <Row>
              <Field name="description">
                {(fp) => (
                  <RForm.Group as={Col} controlId="description">
                    <RForm.Label>
                      <span className="lead">Description</span>{' '}
                      <sup>required</sup>
                    </RForm.Label>
                    <RForm.Control
                      required
                      as="textarea"
                      rows={3}
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
            <Row>
              <Field name="access_license">
                {(fp) => (
                  <RForm.Group as={Col} controlId="access_license">
                    <RForm.Label>
                      <span className="lead">Access License <sup><a href="/policies" target="_blank"><InfoCircle /></a></sup></span>
                    </RForm.Label>
                    <RForm.Control
                      required
                      as="select"
                      value={fp.field.value}
                      onBlur={fp.field.onBlur}
                      onChange={fp.field.onChange}
                      isValid={fp.meta.touched && !fp.meta.error}
                      isInvalid={fp.meta.touched && fp.meta.error}
                      disabled={true}
                    >
                      {Object.values(AccessLicense).map((val) => (
                        <option key={val} value={val}>{val}</option>
                      ))}
                    </RForm.Control>
                  </RForm.Group>
                )}
              </Field>
            </Row>
            <Row>
              <Col >
                <Button
                  className="submit-button"
                  disabled={!isValid}
                  type="submit"
                  variant="primary"
                >
                  Publish
                </Button>
              </Col>
            </Row>
          </RForm>
        )}
      </Formik>
    </React.Fragment>
  );
};

export default PublicationForm;
