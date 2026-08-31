(() => {
    const detailPane = document.getElementById('judgeDetailPane');
    const statusFilter = document.getElementById('judgeStatusFilter');
    const targetRows = Array.from(document.querySelectorAll('.judge-target-row'));
    const filterEmpty = document.getElementById('judgeFilterEmpty');
    const rankingLink = document.getElementById('judgeRankingLink');
    // 상세를 열면 패널이 통째로 교체되므로, 처음 렌더된 순위 화면을 붙잡아 둡니다.
    const rankingHtml = detailPane ? detailPane.innerHTML : '';

    const refreshProgress = () => {
        const submitted = targetRows.filter(row => row.dataset.status === 'SUBMITTED').length;
        const drafts = targetRows.filter(row => row.dataset.status === 'DRAFT').length;
        const submittedCount = document.getElementById('judgeSubmittedCount');
        const draftCount = document.getElementById('judgeDraftCount');
        const remainingCount = document.getElementById('judgeRemainingCount');
        const completedMetric = document.getElementById('judgeCompletedMetric');
        const progressBar = document.getElementById('judgeProgressBar');
        const progressTrack = progressBar?.closest('[role="progressbar"]');
        const percentage = targetRows.length === 0 ? 0 : Math.round(submitted / targetRows.length * 100);
        if (submittedCount) submittedCount.textContent = String(submitted);
        if (completedMetric) completedMetric.textContent = String(submitted);
        if (draftCount) draftCount.textContent = String(drafts);
        if (remainingCount) remainingCount.textContent = String(targetRows.length - submitted);
        if (progressBar) progressBar.style.width = `${percentage}%`;
        if (progressTrack) progressTrack.setAttribute('aria-valuenow', String(percentage));
    };

    const initializeScorePanel = root => {
        const scoreInputs = Array.from(root.querySelectorAll('.judge-score-input'));
        const totalScore = root.querySelector('#judgeTotalScore');
        const tableTotal = root.querySelector('#judgeTableTotal');

        const updateTotal = () => {
            const total = scoreInputs.reduce((sum, input) => {
                const value = Number.parseInt(input.value, 10);
                return sum + (Number.isFinite(value) ? value : 0);
            }, 0);
            if (totalScore) totalScore.textContent = String(total);
            if (tableTotal) tableTotal.textContent = String(total);
        };

        scoreInputs.forEach(input => input.addEventListener('input', updateTotal));
    };

    const setActiveRow = applicationId => {
        targetRows.forEach(row => {
            const active = row.dataset.applicationId === String(applicationId);
            row.classList.toggle('is-active', active);
            if (active) row.setAttribute('aria-current', 'true');
            else row.removeAttribute('aria-current');
        });
    };

    const bindRankingRows = () => {
        detailPane?.querySelectorAll('[data-ranking-application-id]').forEach(link => {
            link.addEventListener('click', event => {
                const row = targetRows.find(
                    item => item.dataset.applicationId === link.dataset.rankingApplicationId
                );
                if (!row) return;
                event.preventDefault();
                loadPanel(row);
            });
        });
    };

    const showRanking = (updateHistory = true) => {
        if (!detailPane || !rankingHtml) return;
        detailPane.innerHTML = rankingHtml;
        bindRankingRows();
        setActiveRow(null);
        if (updateHistory) {
            const nextUrl = new URL(window.location.href);
            nextUrl.search = '';
            window.history.pushState({}, '', nextUrl);
        }
    };

    const renderLoadError = () => {
        if (!detailPane) return;
        detailPane.innerHTML = `
            <div class="card judge-workspace-placeholder judge-workspace-error">
                <strong>평가 화면을 불러오지 못했습니다.</strong>
                <p>잠시 후 다시 선택해 주세요.</p>
            </div>`;
    };

    const loadPanel = async (row, updateHistory = true) => {
        if (!detailPane || !row) return;
        setActiveRow(row.dataset.applicationId);
        detailPane.classList.add('is-loading');

        try {
            const current = new URL(window.location.href);
            const panelUrl = new URL(row.dataset.panelUrl, window.location.origin);
            if (current.searchParams.get('applicationId') === row.dataset.applicationId) {
                if (current.searchParams.has('saved')) panelUrl.searchParams.set('saved', '');
            }
            const response = await fetch(panelUrl, {
                headers: {'X-Requested-With': 'XMLHttpRequest'},
                credentials: 'same-origin'
            });
            if (response.redirected && response.url.includes('/judge/login')) {
                window.location.assign(response.url);
                return;
            }
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            detailPane.innerHTML = await response.text();
            initializeScorePanel(detailPane);

            if (updateHistory) {
                const nextUrl = new URL(window.location.href);
                nextUrl.search = '';
                nextUrl.searchParams.set('applicationId', row.dataset.applicationId);
                window.history.pushState({applicationId: row.dataset.applicationId}, '', nextUrl);
            }
        } catch (error) {
            renderLoadError();
        } finally {
            detailPane.classList.remove('is-loading');
        }
    };

    const filterTargets = () => {
        const status = statusFilter?.value || 'ALL';
        let visibleCount = 0;

        targetRows.forEach(row => {
            const visible = status === 'ALL' || row.dataset.status === status;
            row.hidden = !visible;
            if (visible) visibleCount += 1;
        });

        if (filterEmpty) filterEmpty.hidden = visibleCount > 0 || targetRows.length === 0;
    };

    targetRows.forEach(row => {
        row.addEventListener('click', event => {
            event.preventDefault();
            loadPanel(row);
        });
    });
    statusFilter?.addEventListener('change', filterTargets);
    filterTargets();
    refreshProgress();

    rankingLink?.addEventListener('click', () => showRanking());

    window.addEventListener('popstate', () => {
        const applicationId = new URL(window.location.href).searchParams.get('applicationId');
        const row = targetRows.find(item => item.dataset.applicationId === applicationId);
        if (row) loadPanel(row, false);
        else showRanking(false);
    });

    // 주소에 과제가 지정된 경우에만 상세를 엽니다. 그 밖에는 순위 화면이 첫 화면입니다.
    if (detailPane && targetRows.length > 0) {
        const applicationId = new URL(window.location.href).searchParams.get('applicationId');
        const initialRow = targetRows.find(row => row.dataset.applicationId === applicationId);
        if (initialRow) loadPanel(initialRow, false);
        else bindRankingRows();
    } else {
        initializeScorePanel(document);
    }
})();
