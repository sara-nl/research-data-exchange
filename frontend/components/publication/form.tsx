import * as React from 'react';
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
import FieldFeedback from './fieldFeedback';
import { useState } from 'react';
import Error, { ErrorProps } from 'next/error';

type Props = {
  storeValues: (values: Values) => Promise<Values>;
  onSuccessSubmission: (values: Values) => Promise<void> | void;
  header: string;
};

export type Values = {
  doi: string;
  title: string;
  authors: string;
  description: string;
};

const PublicationForm: React.FC<Props> = ({
  header,
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
        initialValues={{ doi: '', title: '', authors: '', description: '' }}
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
          handleChange,
          handleSubmit,
          handleBlur,
          values,
          errors,
          isValidating,
          isValid,
          dirty,
        }) => (
          <RForm noValidate validated={!errors} onSubmit={handleSubmit}>
            <RForm.Row>
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
            </RForm.Row>
            <RForm.Row>
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
            </RForm.Row>
            <RForm.Row>
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
            </RForm.Row>
            <RForm.Row>
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
            </RForm.Row>
            <Button
              disabled={!dirty || !isValid}
              type="submit"
              variant="primary"
            >
              Publish
            </Button>
          </RForm>
        )}
      </Formik>
    </React.Fragment>
  );
};

export default PublicationForm;
