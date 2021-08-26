import { FieldProps } from 'formik';
import React from 'react';
import { Form as RForm } from 'react-bootstrap';

type Props = FieldProps;

const FieldFeedback: React.FC<Props> = (props: Props) => (
  <React.Fragment>
    <RForm.Control.Feedback type="valid">Looks good!</RForm.Control.Feedback>
    {props.meta.touched && props.meta.error && (
      <RForm.Control.Feedback type="invalid">
        {props.meta.error}
      </RForm.Control.Feedback>
    )}
  </React.Fragment>
);

export default FieldFeedback;
