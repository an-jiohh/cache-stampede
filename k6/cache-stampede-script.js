import http from 'k6/http';
import { sleep } from 'k6';

export let options = {
    vus: 9000,           // 전체 가상 유저 300명 (원하면 조절)
    duration: '10m',    // 1분 동안 실행
};

const BASE_URL = 'http://localhost:8080/item/';

export default function () {
    const vu = __VU;
    const USERS_PER_KEY = 90;

    const key = Math.floor((vu - 1) / USERS_PER_KEY) + 1;

    const res = http.get(`${BASE_URL}${key}`);

    if (res.status !== 200) {
        console.error(`Error: status=${res.status} key=${key} error=${res.error}`);
    }

    sleep(0.3);
}