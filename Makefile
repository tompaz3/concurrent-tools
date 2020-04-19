install:
	./mvnw install

clean-install:
	./mvnw clean install

DEPLOY_VERSION=$(shell echo ${VERSION} | grep -o -E '([0-9]+\.){2}[0-9]+')
deploy:
	@echo "${DEPLOY_VERSION}"
ifneq (${DEPLOY_VERSION},)
	git tag ${DEPLOY_VERSION}
	mvn deploy -P push-to-repo
else
	@echo "Invalid verision: ${VERSION}"
endif
