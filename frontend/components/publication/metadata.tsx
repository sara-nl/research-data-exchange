import * as React from 'react';
import {
  Row,
  Button,
  Col,
  Form as RForm,
} from 'react-bootstrap';
import { InfoCircle } from 'react-bootstrap-icons';
import { Formik, Field, ErrorMessage } from 'formik';
import FieldFeedback from './../form/fieldFeedback';
import { useState } from 'react';
import { Dataset, Metadata } from '../../types';
import * as Yup from 'yup';

type Props = {
  storeValues: (values: Values) => Promise<Metadata>;
  onSuccessSubmission: (metadata: Metadata) => Promise<void> | void;
};

export type Values = {
  url: string;
};

const validationSchema = Yup.object().shape({
    url: Yup.string()
        .url('Please enter a valid URL (e.g. https://example.figshare.com/articles/dataset/RDX_Example/12345)')
        .required('URL is required')
})

const MetadataForm: React.FC<Props> = ({
  storeValues,
  onSuccessSubmission,
}) => {


  return (
    <React.Fragment>
    <Row className='mb-5'>
      <Formik
        initialValues={{
          url: '',
        }}
        validateOnMount={true}
        validateOnChange={true}
        validateOnBlur={false}
        validationSchema={validationSchema}
        onSubmit={async (values, actions) => {
          await storeValues(values)
            .catch((e) => {
                actions.setFieldError('url', JSON.parse(e).detail);
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
              <Field name="url">
                {(fp) => (
                  <RForm.Group as={Col} controlId="url">
                    <RForm.Label>
                      <span className="lead">
                        Retrieve metadata from Figshare
                      </span>
                      <sup className="font-weight-light"></sup>
                    </RForm.Label>
                    <RForm.Control
                      required
                      type="text"
                      value={fp.field.value}
                      onBlur={fp.field.onBlur}
                      onChange={fp.field.onChange}
                      isValid={fp.meta.touched && !fp.meta.error}
                      isInvalid={fp.meta.touched && fp.meta.error}
                      placeholder="Enter Figshare URL"
                    />
                    <FieldFeedback {...fp} />
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
                  Fetch metadata
                </Button>
              </Col>
            </Row>
          </RForm>
        )}
      </Formik>
      </Row>
    </React.Fragment>
  );
};

export default MetadataForm;
