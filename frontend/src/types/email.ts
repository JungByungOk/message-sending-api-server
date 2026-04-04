export interface MessageTag {
  name: string;
  value: string;
}

export interface SendEmailRequest {
  from: string;
  to: string[];
  subject: string;
  body: string;
  tags?: MessageTag[];
}

export interface SendTemplatedEmailRequest {
  templateName: string;
  from: string;
  to: string[];
  cc?: string[];
  bcc?: string[];
  templateData: Record<string, string>;
  tags?: MessageTag[];
}

export interface EmailTemplate {
  templateName: string;
  subjectPart: string;
  htmlPart: string;
  textPart: string;
}

export interface DeleteTemplateRequest {
  templateName: string;
}

export interface SendEmailResponse {
  messageId: string;
}

export interface TemplateResponse {
  awsRequestId: string;
}

export interface TemplateMetadata {
  name: string;
  createdTimestamp: string;
}
