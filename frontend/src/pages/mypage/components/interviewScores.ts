import { SCORE_ITEMS } from '../../../constants/interviewEvaluation';
import type { InterviewResultDTO } from "../../../types/api";

export { SCORE_ITEMS };

export function formatScore(value: unknown) {
    if (value === null || value === undefined || value === '' || !Number.isFinite(Number(value))) return '-';
    return Number(value).toLocaleString('ko-KR', { maximumFractionDigits: 1 });
}

export function formatInterviewDate(value: string | null | undefined) {
    return value ? value.slice(0, 16).replace('T', ' ').replaceAll('-', '.') : '날짜 없음';
}

export function sortInterviewResults<T extends {interviewDate?: string; historyNum: number}>(results: T[]) {
    return [...results].sort((a, b) => (
        String(b.interviewDate || '').localeCompare(String(a.interviewDate || ''))
        || Number(b.historyNum) - Number(a.historyNum)
    ));
}

export function radarPoint(index: number, value: unknown, radius = 110) {
    const numeric = Number(value);
    const score = Number.isFinite(numeric) ? Math.min(100, Math.max(0, numeric)) : 0;
    const angle = -Math.PI / 2 + index * Math.PI * 2 / SCORE_ITEMS.length;
    return [200 + Math.cos(angle) * radius * score / 100, 180 + Math.sin(angle) * radius * score / 100];
}

export function radarPolygon(result: InterviewResultDTO) {
    return SCORE_ITEMS.map((item, index) => radarPoint(index, result[item.key]).join(',')).join(' ');
}
