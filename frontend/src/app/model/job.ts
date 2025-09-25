import { StatusJob } from "../enum/status-job";

export interface Job {
    id: number;
    nome: string;
    cronExpression: string;
    status: StatusJob;
    ultimaExecucao?: Date;
    proximaExecucao?: Date;
}