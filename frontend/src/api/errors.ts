import axios from 'axios';
import type { ApiErrorData, Id } from '../types/api';

export interface RequestError<T = ApiErrorData> extends Error {
  code?: string;
  response?: { status: number; data?: T };
  attachmentSaveUncertain?: boolean;
  createdPostNum?: Id;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function readErrorBody(value: unknown): ApiErrorData | undefined {
  if (!isRecord(value)) return undefined;
  const result: ApiErrorData = {};
  if (typeof value.message === 'string') result.message = value.message;
  if (isRecord(value.responseCode)) {
    result.responseCode = {};
    if (typeof value.responseCode.code === 'number') result.responseCode.code = value.responseCode.code;
    if (typeof value.responseCode.message === 'string') result.responseCode.message = value.responseCode.message;
  }
  if (typeof value.data === 'string') result.data = value.data;
  else if (isRecord(value.data)) {
    const data = value.data;
    result.data = {
      message: typeof data.message === 'string' ? data.message : undefined,
      maintenance: data.maintenance === true,
      restartRequired: data.restartRequired === true,
      answerLocked: data.answerLocked === true,
    };
  }
  return result;
}

/** Keep the original error (including upload recovery metadata) when it is an Error. */
export function requestError(value: unknown): RequestError {
  if (axios.isAxiosError<ApiErrorData>(value)) return value;
  if (value instanceof Error) return value;
  if (isRecord(value)) {
    const error: RequestError = new Error(typeof value.message === 'string' ? value.message : '요청을 처리하지 못했습니다.');
    if (typeof value.code === 'string') error.code = value.code;
    if (value.attachmentSaveUncertain === true) error.attachmentSaveUncertain = true;
    if (typeof value.createdPostNum === 'number' || typeof value.createdPostNum === 'string') error.createdPostNum = value.createdPostNum;
    const response = value.response;
    if (isRecord(response) && typeof response.status === 'number') {
      error.response = { status: response.status, data: readErrorBody(response.data) };
    }
    return error;
  }
  return new Error(String(value));
}

export function errorDetails(value: unknown) {
  const data = requestError(value).response?.data?.data;
  return typeof data === 'object' && data !== null ? data : {};
}
