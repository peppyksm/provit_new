import client from './client';
import type { ApiResponse, TodayFortuneDTO } from "../types/api";
import { requestError } from "./errors";

/**
 * 오늘의 취업 운세 및 맞춤 추천 공고 조회
 * @returns {Promise<Object>} ApiResponse<TodayFortuneDTO>
 */
export const fetchTodayFortune = async () => {
    try {
        const response = await client.get<ApiResponse<TodayFortuneDTO>>('/fortune/today');
        return response.data;
    } catch (caughtError) {
        const error = requestError(caughtError);
        console.error('오늘의 취업 운세 조회 실패:', error);
        throw error;
    }
};
