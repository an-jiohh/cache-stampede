import http from 'k6/http';
import { sleep } from 'k6';

export let options = {
    vus: 9000,
    duration: '10m',
};

const BASE_URL = 'http://localhost:8080/item/1';

export default function () {
    const res = http.get(BASE_URL);

    if (res.status !== 200) {
        console.error(`Error: status=${res.status}`);
    }
    // console.log(res)

    sleep(0.3);
}