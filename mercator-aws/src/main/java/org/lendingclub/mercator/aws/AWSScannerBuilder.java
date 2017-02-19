package org.lendingclub.mercator.aws;

import java.lang.reflect.InvocationTargetException;

import org.lendingclub.mercator.core.Projector;
import org.lendingclub.mercator.core.ScannerBuilder;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public class  AWSScannerBuilder extends ScannerBuilder<AWSScanner> {


	private Supplier<String> acccountIdSupplier = Suppliers.memoize(new AccountIdSupplier());
	private Region region;
	private AWSCredentialsProvider credentialsProvide;
	private ClientConfiguration clientConfiguration;
	private Class<? extends AWSScanner> targetType;
	
	public AWSScannerBuilder() {
		
	}

	AWSCredentialsProvider getCredentialsProvider() {
		if (credentialsProvide==null) {
			return new DefaultAWSCredentialsProviderChain();
		}
		return credentialsProvide;
	}
	
	public Supplier<String> getAccountIdSupplier() {
		return acccountIdSupplier;
	}
	class AccountIdSupplier implements Supplier<String> {

		@Override
		public String get() {
			AWSSecurityTokenServiceClientBuilder b = AWSSecurityTokenServiceClientBuilder.standard().withCredentials(getCredentialsProvider());
			
			// this will fail if the region is not set
			if (region!=null) {
				b = b.withRegion(region.getName());
			}
			else {
				b = b.withRegion(Regions.US_EAST_1); 
			}
			if (clientConfiguration!=null) {
				b = b.withClientConfiguration(clientConfiguration);
			}
			AWSSecurityTokenService svc = b.build();
			
		
			GetCallerIdentityResult result = svc.getCallerIdentity(new GetCallerIdentityRequest());
			
			return result.getAccount();
		}
		
	}
	
	public AWSScannerBuilder withRegion(String region) {
		return withRegion(Regions.fromName(region));
	}
	
	public AWSScannerBuilder withRegion(Regions r) {
	
		Preconditions.checkState(this.region == null, "region already set");
		this.region = Region.getRegion(r);
		return this;
	}

	public AWSScannerBuilder withRegion(Region r) {
		Preconditions.checkState(this.region == null, "region already set");
		this.region = r;
		return this;
	}

	public AWSScannerBuilder withAccountId(final String id) {
		this.acccountIdSupplier = new Supplier<String>() {

			@Override
			public String get() {
				return id;
			}

			
		};
		return this;
	}

	
	public AWSScannerBuilder withCredentials(AWSCredentialsProvider p) {
		this.credentialsProvide = p;
		return this;
	}
	
	public AWSScannerBuilder withProjector(Projector p) {
		Preconditions.checkState(this.getProjector() == null, "projector already set");
		setProjector(p);
		return this;
	}

	public AwsClientBuilder configure(AwsClientBuilder b) {
	
		b = b.withRegion(Regions.fromName(region.getName())).withCredentials(getCredentialsProvider());
		if (clientConfiguration != null) {
			b = b.withClientConfiguration(clientConfiguration);
		}
		return b;
	}

	private void checkRequiredState() {
		Preconditions.checkState(getProjector() != null, "projector not set");
		Preconditions.checkState(region != null, "region not set");
	}

	public ASGScanner buildASGScanner() {
		checkRequiredState();
		return build(ASGScanner.class);
	}

	public AMIScanner buildAMIScanner() {
		checkRequiredState();
		return build(AMIScanner.class);
	}

	public VPCScanner buildVPCScanner() {
		checkRequiredState();
		return build(VPCScanner.class);
	}

	public AccountScanner buildAccountScanner() {
		return build(AccountScanner.class);
	}


	public SecurityGroupScanner buildSecurityGroupScanner() {
		return build(SecurityGroupScanner.class);
	}

	public SubnetScanner buildSubnetScanner() {
		return build(SubnetScanner.class);
	}

	public RDSInstanceScanner buildRDSInstanceScanner() {
		 return build(RDSInstanceScanner.class);
	}
	
	public ELBScanner buildELBScanner() {
		return build(ELBScanner.class);
	}


	public <T extends AWSScanner> T build(Class<T> clazz) {
		try {
			this.targetType =  (Class<AWSScanner>) clazz;
			return (T) clazz.getConstructor(AWSScannerBuilder.class).newInstance(this);
		} catch (IllegalAccessException | InstantiationException | InvocationTargetException
				| NoSuchMethodException e) {
			throw new IllegalStateException(e);
		}

	}

	@Override
	public AWSScanner<AmazonWebServiceClient> build() {
		Preconditions.checkState(targetType!=null,"target type not set");
		return (AWSScanner<AmazonWebServiceClient>) build(this.targetType);
		
	}
	
	public Region getRegion() {
		return region;
	}
	
}
