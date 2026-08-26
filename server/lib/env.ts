export class ApiError extends Error {
  status: number;
  code: string;

  constructor(status: number, message: string, code = 'error') {
    super(message);
    this.status = status;
    this.code = code;
  }
}

export function requireEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new ApiError(500, `Missing environment variable: ${name}`, 'missing_env');
  }
  return value;
}
